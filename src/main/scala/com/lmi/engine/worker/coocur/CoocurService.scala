package com.lmi.engine.worker.coocur

import com.lmi.engine.EngineThreads
import com.lmi.engine.graph._
import com.lmi.engine.worker.util.Ids
import com.lmi.engine.worker.util.ignite.{BatchLatch, BatchLatchFactory, CappedIterator}
import org.apache.ignite.cache.query.{SqlFieldsQuery, SqlQuery}
import org.apache.ignite.cache.{CacheEntry, CacheMode}
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.lang.{IgniteClosure, IgniteFuture, IgniteInClosure}
import org.apache.ignite.{Ignite, IgniteCache}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise
import scala.util.Success

object CoocurUtil {
	
	def getPairKeyFor(obj1Id: Id[_], obj2Id: Id[_]): String = {
		val min = if(obj1Id.id.compareTo(obj2Id.id) < 0) obj1Id.id else obj2Id.id
		val max = if(obj1Id.id.compareTo(obj2Id.id) >= 0) obj1Id.id else obj2Id.id
		
		Ids.createKeyFrom(min, max)
	}
	
	def getKeyFor(pair: CoocurrencePair): String = {
		Ids.createKeyFrom(pair.getNodeAId, pair.getNodeBId)
	}
	
	def obj1IdFromKey(key: String): String = {
		Ids.getIdFromKey(key, 0)
	}
	
	def obj2IdFromKey(key: String): String = {
		Ids.getIdFromKey(key, 1)
	}
	
	def getGlobalCounterName
	(relation: Relation[_, _, _]): String = {
		"Global_" + relation.id
	}
	
	def getPairCacheConfigFor
	(relation: Relation[_, _, _]): CacheConfiguration[String, CoocurrencePair] = {
		val cacheConfig = new CacheConfiguration[String, CoocurrencePair]()
		cacheConfig.setName(getPairCacheNameFor(relation))
		cacheConfig.setCacheMode(CacheMode.PARTITIONED)
		cacheConfig.setBackups(0)
		cacheConfig.setIndexedTypes(classOf[String], classOf[CoocurrencePair])
		
		cacheConfig
	}
	
	def getPairCacheNameFor
	(relation: Relation[_, _, _]): String = {
		"Coocur_" + relation.id
	}
	
	def getOriginObjCacheConfigFor(relation: Relation[_, _, _]):
	CacheConfiguration[String, Integer] = {
		val cacheConfig = new CacheConfiguration[String, Integer]()
		cacheConfig.setName(getOriginObjCacheNameFor(relation))
		cacheConfig.setCacheMode(CacheMode.PARTITIONED)
		cacheConfig.setBackups(0)
		
		cacheConfig
	}
	
	def getOriginObjCacheNameFor(relation: Relation[_, _, _]): String = {
		"Obj_" + relation.id
	}
	
	def getOtherIdFromPairKey(key: String, objId: Id[_]): String = {
		val id1 = Ids.getIdFromKey(key, 0)
		if(id1.equals(objId.id))
			Ids.getIdFromKey(key, 1)
		else
			id1
	}
	
}

class CoocurService[T <: Edge, ORIGIN <: Node, DEST <: Node]
(relation: Relation[ORIGIN, T, DEST])(implicit ignite: Ignite) {
	
	val pairCacheName: String = CoocurUtil.getPairCacheNameFor(relation)
	private val globalCount =
		ignite.atomicLong(CoocurUtil.getGlobalCounterName(relation), 0, true)
	private val pairCount: IgniteCache[String, CoocurrencePair] =
		ignite.getOrCreateCache(CoocurUtil.getPairCacheConfigFor(relation))
	private val nodeCount =
		ignite.getOrCreateCache(CoocurUtil.getOriginObjCacheConfigFor(relation))
	
	def incrementCoocurrencesAsync
	(coocurringOriginIds: Iterator[Id[ORIGIN]],
	 originId: Id[ORIGIN],
	 destinationId: Id[DEST],
	 returnLatch: Boolean = false): Option[BatchLatch] = {
		
		val latch = BatchLatchFactory.newOptional(returnLatch)
		
		//For every Obj (B) that coocurs with the objId (A) needs to have their
		//1.) A-and-B coocurrence incremented
		//2.) Individual A and individual B count of how many times they globally coocurred incremented
		//3.) Increment the global occurrences of A and B
		val matchingTargets = new ListBuffer[Id[ORIGIN]]()
		
		coocurringOriginIds.foreach(coocurredOriginId => {
			if(!originId.equals(coocurredOriginId)) {
				matchingTargets += coocurredOriginId
			}
		})
		
		if(matchingTargets.nonEmpty) {
			val batchLatch = new BatchLatch()
			
			globalCount.addAndGet(matchingTargets.length)
			
			matchingTargets.foreach(matchingId => {
				val future = nodeCount.invokeAsync(matchingId.id, new IncrementObjClosure)
				batchLatch.listenForFuture(future)
			})
			
			val originUpdate = nodeCount.invokeAsync(originId.id,
				new IncrementObjClosure(), Int.box(matchingTargets.length))
			batchLatch.listenForFuture(originUpdate)
			
			//latchDelayer to keep latch from completing before increment jobs are added
			val latchDelayer = latch.map(_ => Promise[Boolean]())
			latch.foreach(_.listenForFuture(latchDelayer.get.future))
			
			batchLatch.setOnComplete(() => {
				//Set final state to trigger state update handler in Ignite
				matchingTargets.foreach(matchingId => {
					val key = CoocurUtil.getPairKeyFor(matchingId, originId)
					val incremented = pairCount.invokeAsync(key, new IncrementPairClosure)
					latch.foreach(_.listenForFuture(incremented))
				})
				//Allow returned latch to complete
				latchDelayer.foreach(
					_.complete(Success(true))
				)
			})
		}
		
		latch
	}
	
	def getGlobalCount: Long = {
		globalCount.get
	}
	
	def getPairCount(objId1: Id[ORIGIN], objId2: Id[ORIGIN]): Int = {
		val key = CoocurUtil.getPairKeyFor(objId1, objId2)
		val pair = pairCount.get(key)
		if(pair != null)
			pair.getCoocurCount
		else 0
	}
	
	def getObjCounts(objAId: Id[ORIGIN], objBId: Id[ORIGIN]): Promise[(Integer, Integer)] = {
		val count1 = nodeCount.getAsync(objAId.id)
		val count2 = nodeCount.getAsync(objBId.id)
		
		val counts = Array[Integer](null, null)
		val futureCounts = Promise[(Integer, Integer)]()
		
		count1.listenAsync(new IgniteInClosure[IgniteFuture[Integer]] {
			override def apply(e: IgniteFuture[Integer]): Unit = {
				counts synchronized {
					counts(0) =
						if(e.get() == null) 0 else e.get()
					if(counts(1) != null) {
						futureCounts.success((counts(0), counts(1)))
					}
				}
			}
		}, EngineThreads.coocurrencePool)
		
		count2.listenAsync(new IgniteInClosure[IgniteFuture[Integer]] {
			override def apply(e: IgniteFuture[Integer]): Unit = {
				counts synchronized {
					counts(1) =
						if(e.get() == null) 0 else e.get()
					if(counts(0) != null) {
						futureCounts.success((counts(0), counts(1)))
					}
				}
			}
		}, EngineThreads.coocurrencePool)
		
		futureCounts
	}
	
	def getCountAsync(nodeId: Id[ORIGIN]): IgniteFuture[Int] = {
		nodeCount.getAsync(nodeId.id).chain(new IgniteClosure[IgniteFuture[Integer], Int] {
			override def apply(e: IgniteFuture[Integer]): Int = {
				if(e.get() == null)
					0
				else
					e.get()
			}
		})
	}
	
	def getCoocurrencesWith(nodeId: Id[ORIGIN]): java.util.Iterator[CacheEntry[String, CoocurrencePair]] = {
		val query = new SqlQuery(classOf[CoocurrencePair],
			CoocurrencePair.NODE_A_ID_FIELD + " = ? OR " +
				CoocurrencePair.NODE_B_ID_FIELD + " = ?")
		query.setArgs(nodeId.id, nodeId.id)
		
		pairCount.query(query).iterator()
			.asInstanceOf[java.util.Iterator[CacheEntry[String, CoocurrencePair]]]
	}
	
	def removeObjAsync(objId: Id[ORIGIN], returnLatch: Boolean = false): Option[BatchLatch] = {
		val latch = BatchLatchFactory.newOptional(returnLatch)
		val matchedKeys = getPairKeysFor(objId)
		
		//Remove each pair matching objId then decrement the other Id's coocurrence count
		matchedKeys.foreach(matchedKey => {
			val otherObjId = CoocurUtil.getOtherIdFromPairKey(matchedKey, objId)
			val pairRemoved = pairCount.getAndRemoveAsync(matchedKey)
				.chain(new IgniteClosure[IgniteFuture[CoocurrencePair], Unit] {
					override def apply(e: IgniteFuture[CoocurrencePair]): Unit = {
						val decrementAmount = Int.box(-e.get.getCoocurCount)
						val otherDecrememted =
							nodeCount.invokeAsync(otherObjId, new IncrementObjClosure(), decrementAmount)
						latch.foreach(_.listenForFuture(otherDecrememted))
					}
				})
			latch.foreach(_.listenForFuture(pairRemoved))
		})
		
		//Remove objId from obj cache and use removed value to decrement global count
		val objRemovedAndGlobalDecremented = nodeCount.getAndRemoveAsync(objId.id)
			.chain(new IgniteClosure[IgniteFuture[Integer], Unit] {
				override def apply(e: IgniteFuture[Integer]): Unit = {
					if(e.get != null)
						globalCount.addAndGet(-e.get)
				}
			})
		
		latch.foreach(_.listenForFuture(objRemovedAndGlobalDecremented))
		latch
	}
	
	private def getPairKeysFor(objId: Id[ORIGIN]): Iterator[String] = {
		val query = new SqlFieldsQuery("SELECT _key FROM " +
			classOf[CoocurrencePair].getSimpleName +
			" WHERE " +
			CoocurrencePair.NODE_A_ID_FIELD + " = ? OR " +
			CoocurrencePair.NODE_B_ID_FIELD + " = ?")
		query.setArgs(objId.id, objId.id)
		
		new CappedIterator(pairCount.query(query).iterator(), Int.MaxValue)
	}
	
}
