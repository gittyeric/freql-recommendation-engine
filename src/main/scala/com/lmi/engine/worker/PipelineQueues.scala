package com.lmi.engine.worker

import java.util.concurrent.TimeUnit

import com.lmi.engine.EngineThreads
import com.lmi.engine.graph.{Edge, Id, Node}
import com.lmi.engine.worker.coocur.{CoocurService, CoocurUtil, CoocurrencePair}
import com.lmi.engine.worker.history.{HistoryEntry, HistoryService, HistoryUtil}
import com.lmi.engine.worker.lru.LRUService
import com.lmi.engine.worker.output.stream.ReactiveQueryStream
import com.lmi.engine.worker.similar.SimilarityService
import com.lmi.engine.worker.util.ignite.{BatchLatch, IgniteEventUtil}
import org.apache.ignite.events.{CacheEvent, EventType}
import org.apache.log4j.Logger
import org.jsr166.ConcurrentLinkedHashMap

class PipelineQueues[ORIGIN <: Node, E <: Edge, DEST <: Node]
(
	originLRU: Option[LRUService[ORIGIN]],
	destLRU: Option[LRUService[DEST]],
	history: HistoryService[ORIGIN, E, DEST],
	coocurrences: CoocurService[E, ORIGIN, DEST],
	similarities: SimilarityService[ORIGIN, E, DEST],
	maxRecsPerTarget: Int,
	reactiveOutputs: Seq[ReactiveQueryStream[ORIGIN, _, _]] = Seq()
)
(implicit context: PipelineContext) {
	
	//------ Pipeline code: History updates invoke Coocurrence updates which invoke Similarity updates ---------
	
	private implicit def lambdaToRunnable(lambda: () => Unit): Runnable =
		new Runnable {
			override def run(): Unit = lambda()
		}
	
	implicit val ignite = context.ignite
	val tuning = context.tuning
	val maxParaHistory = tuning.MAX_EXPECTED_PARALLEL_HISTORY_UPDATES
	val maxParaCoocur = Math.min((maxParaHistory * Math.sqrt(maxParaHistory)).toInt, Integer.MAX_VALUE / 2)
	private val logger = Logger.getLogger("RecommendPipeline")
	//Local queues for asyncronous Ignite event processing (with deduplication of pending queue by key)
	private val updatedHistoryQueue =
		Queue[HistoryEntry](maxParaHistory.toInt)
	private val updatedCoocurQueue =
		Queue[CoocurrencePair](maxParaCoocur.toInt)
	private val updatedSimilarityQueue =
		Queue[Option[BatchLatch]](maxParaCoocur.toInt)
	private val evictedOriginQueue =
		Queue[String](
			Math.max(maxParaHistory / 10, 100).toInt)
	private val evictedDestQueue =
		Queue[String](Math.max(maxParaHistory / 10, 100).toInt)
	
	def start(): Unit = {
		listenForCacheChanges()
		startDequeuingThreads()
	}
	
	private def listenForCacheChanges(): Unit = {
		
		val isInverse = history.relation.isInverse
		//Listen for Step 1: Every history update triggers Coocurrence updates
		IgniteEventUtil.getPipe(EventType.EVT_CACHE_OBJECT_PUT)
			.listen(history.historyCacheName,
				(event: CacheEvent) => {
					event.newValue() match {
						case historyEntry: HistoryEntry =>
							val key = HistoryUtil.getHistoryKeyFor(historyEntry, isInverse)
							updatedHistoryQueue.put(key, historyEntry)
						case _ => logger.error("Unknown type in History cache: " + event.newValue().toString)
					}
				})
		
		//Listen for Step 2: Every coocurrence update triggers Similarity updates
		IgniteEventUtil.getPipe(EventType.EVT_CACHE_OBJECT_PUT)
			.listen(coocurrences.pairCacheName,
				(event: CacheEvent) => {
					event.newValue() match {
						case coocurrence: CoocurrencePair =>
							updatedCoocurQueue.put(CoocurUtil.getKeyFor(coocurrence), coocurrence)
						case _ => logger.error("Unknown type in Coocurrence cache: " + event.newValue().toString)
					}
				})
		
		//Listen for when targets get rotated out of memory and propagate to relevant caches
		originLRU.foreach(lru => IgniteEventUtil.getPipe(EventType.EVT_CACHE_ENTRY_EVICTED)
			.listen(lru.cacheName(),
				(event: CacheEvent) => {
					val originId: String = event.key()
					evictedOriginQueue.put(originId, originId)
				}
			))
		
		//Listen for when items get rotated out of memory and propagate to history cache
		destLRU.foreach(lru => IgniteEventUtil.getPipe(EventType.EVT_CACHE_ENTRY_EVICTED)
			.listen(lru.cacheName(),
				(event: CacheEvent) => {
					val destinationId = event.key()
					evictedDestQueue.put(destinationId, destinationId)
				}
			))
	}
	
	private def startDequeuingThreads(): Unit = {
		
		//1. Propagate History updates to coocurrence caches
		EngineThreads.historyPool.scheduleWithFixedDelay(() => {
			updatedHistoryQueue.popValues((historyEntry: HistoryEntry) => {
				handleHistoryUpdate(historyEntry)
			})
		}, 0, tuning.HISTORY_UPDATE_DELAY_MS, TimeUnit.MILLISECONDS)
		
		//2. Propagate Coocurrence updates to similarity cache
		EngineThreads.historyPool.scheduleWithFixedDelay(() => {
			updatedCoocurQueue.popValues((coocurrence: CoocurrencePair) => {
				handleCoocurrenceUpdate(coocurrence)
			})
		}, 0, tuning.HISTORY_UPDATE_DELAY_MS, TimeUnit.MILLISECONDS)
		
		//3a. Trim excessive similarities by dirty originId occasionally
		//3b. Propagate updated Obj instances to any change processors
		EngineThreads.similarityPool.scheduleWithFixedDelay(() => {
			updatedSimilarityQueue.popKeyValues((originId, latch) => {
				handleSimilarityUpdate(originId, latch)
			})
		}, tuning.SIMILARITY_TRIM_DELAY_MS,
			tuning.SIMILARITY_TRIM_DELAY_MS, TimeUnit.MILLISECONDS)
		
		//Step 4a. Propagate LRU origin evictions to relevant caches
		EngineThreads.evictionPool.scheduleWithFixedDelay(() => {
			evictedOriginQueue.popValues((originId: String) => {
				handleOriginEviction(originId)
			})
		}, 0, tuning.OBJ_EVICTION_DELAY_MS, TimeUnit.MILLISECONDS)
		
		//Step 4b. Propagate LRU destination evictions to history cache
		EngineThreads.evictionPool.scheduleWithFixedDelay(() => {
			evictedDestQueue.popValues((destinationId: String) => {
				handleDestEviction(destinationId)
			})
		}, 0, tuning.OBJ_EVICTION_DELAY_MS, TimeUnit.MILLISECONDS)
		
	}
	
	//Step 1, history database was updated so now update coocurrences of all related targets
	private def handleHistoryUpdate(historyEntry: HistoryEntry) = {
		logger.trace("History: " + historyEntry.getOriginId + " -> " + historyEntry.getDestinationId)
		
		val originId = history.getOriginIdFrom(historyEntry)
		val destinationId = history.getDestinationIdFrom(historyEntry)
		val coocurrencesOnDest = history.getOriginIdsFor(destinationId)
		
		originLRU.foreach(_.touchAsync(originId))
		destLRU.foreach(_.touchAsync(destinationId))
		coocurrences.incrementCoocurrencesAsync(coocurrencesOnDest, originId, destinationId)
	}
	
	//Step 2, New coocurrence was added or incremented, now compute and save similarity score
	//if above coocurrence min count threshold
	private def handleCoocurrenceUpdate(updatedPair: CoocurrencePair): Unit = {
		logger.trace("Coocurrence: " + updatedPair.getNodeAId + " with " + updatedPair.getNodeBId)
		
		if(maxRecsPerTarget > 0) {
			val objAId = Id[ORIGIN](updatedPair.getNodeAId)
			val objBId = Id[ORIGIN](updatedPair.getNodeBId)
			
			coocurrences.getObjCounts(objAId, objBId).future
				.foreach(objCoocurrences => {
					
					val latch = similarities.updateScoresAsync(
						objAId, objBId, updatedPair.getCoocurCount(),
						coocurrences.getGlobalCount,
						objCoocurrences._1.toLong,
						objCoocurrences._2.toLong,
						returnLatch = true
					)
					
					//Enqueue these targets to get their bottom similarities trimmed at some point
					updatedSimilarityQueue.put(updatedPair.getNodeAId, latch)
					updatedSimilarityQueue.put(updatedPair.getNodeBId, latch)
				})(EngineThreads.coocurrencePoolContext)
		}
		else {
			updatedSimilarityQueue.put(updatedPair.getNodeAId, None)
			updatedSimilarityQueue.put(updatedPair.getNodeBId, None)
		}
	}
	
	private def handleSimilarityUpdate(simOriginId: String, scoreUpdateLatch: Option[BatchLatch]) = {
		logger.trace("Similarity: " + simOriginId)
		
		val afterSimUpdated = () => {
			if(maxRecsPerTarget > 0) {
				similarities.trimBottomScoresAsync(simOriginId, maxRecsPerTarget)
			}
			reactiveOutputs.foreach(stream =>
				stream.react(Id(simOriginId)))
		}
		
		if(scoreUpdateLatch.isDefined)
			scoreUpdateLatch.get.setOnComplete(afterSimUpdated)
		else
			afterSimUpdated()
	}
	
	private def handleOriginEviction(originId: String) = {
		logger.trace("Evicted " + originId)
		
		val asTypedId = Id[ORIGIN](originId)
		history.removeByOriginIdAsync(asTypedId)
		coocurrences.removeObjAsync(asTypedId)
		similarities.removeScoresAsync(originId)
	}
	
	private def handleDestEviction(destId: String) = {
		history.removeByDestinationIdAsync(Id(destId))
	}
	
	private case class Queue[V](cap: Int) extends ConcurrentLinkedHashMap[String, V](
		cap, ConcurrentLinkedHashMap.DFLT_LOAD_FACTOR, EngineThreads.THREAD_PRIORITY_HIGH - 1) {
		
		def popValues(consumer: V => Unit): Unit = {
			val keyEnum = keys()
			while (keyEnum.hasMoreElements) {
				val nextKey = keyEnum.nextElement()
				val taken = remove(nextKey)
				if(taken != null) {
					consumer(taken)
				}
				else {
					throw new RuntimeException("Dequeued element was null for key " + nextKey)
				}
			}
		}
		
		def popKeyValues(consumer: (String, V) => Unit): Unit = {
			val keyEnum = keys()
			while (keyEnum.hasMoreElements) {
				val nextKey = keyEnum.nextElement()
				val taken = remove(nextKey)
				if(taken != null) {
					consumer(nextKey, taken)
				}
				else {
					throw new RuntimeException("Dequeued element was null for key " + nextKey)
				}
			}
		}
		
	}
	
}

