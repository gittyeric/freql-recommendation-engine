package com.lmi.engine.worker.history

import com.lmi.engine.graph._
import com.lmi.engine.worker.util.Ids
import com.lmi.engine.worker.util.ignite.{BatchLatch, BatchLatchFactory, CappedIterator}
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.query.SqlFieldsQuery
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.lang.IgniteFuture
import org.apache.ignite.{Ignite, IgniteCache}

object HistoryUtil {
	val HISTORY_CACHE_PREFIX = "History_"
	
	def getHistoryKeyFor(historyEntry: HistoryEntry, isInverse: Boolean): String = {
		getHistoryKeyFor(historyEntry.getOriginId, historyEntry.getDestinationId, isInverse)
	}
	
	def getHistoryKeyFor(originId: String, destinationId: String, isInverse: Boolean): String = {
		if(isInverse)
			Ids.createKeyFrom(destinationId, originId)
		else
			Ids.createKeyFrom(originId, destinationId)
	}
	
	def newHistoryFor(originId: String, destinationId: String, isInverse: Boolean): HistoryEntry = {
		if(isInverse)
			new HistoryEntry(destinationId, originId)
		else
			new HistoryEntry(originId, destinationId)
	}
	
	def getOriginIdFromHistoryKey(key: String, isInverse: Boolean): String = {
		if(isInverse)
			Ids.getIdFromKey(key, 1)
		else
			Ids.getIdFromKey(key, 0)
	}
	
	def getDestinationIdFromHistoryKey(key: String, isInverse: Boolean): String = {
		getDestinationIdFromHistoryKey(key, !isInverse)
	}
	
	def getHistoryCacheConfigFor(relation: Relation[_, _, _]): CacheConfiguration[String, HistoryEntry] = {
		val cacheConfig = new CacheConfiguration[String, HistoryEntry]()
		cacheConfig.setName(getHistoryCacheNameFor(relation))
		cacheConfig.setCacheMode(CacheMode.PARTITIONED)
		cacheConfig.setBackups(0)
		cacheConfig.setIndexedTypes(classOf[String], classOf[HistoryEntry])
		
		cacheConfig
	}
	
	def getHistoryCacheNameFor(relation: Relation[_, _, _]): String = {
		HistoryUtil.HISTORY_CACHE_PREFIX + relation.bidirectionalId
	}
	
	private def getDestinationIdFor(historyEntry: HistoryEntry, isInverse: Boolean): String = {
		getOriginIdFor(historyEntry, !isInverse)
	}
	
	private def getOriginIdFor(historyEntry: HistoryEntry, isInverse: Boolean): String = {
		if(isInverse)
			historyEntry.getDestinationId
		else
			historyEntry.getOriginId
	}
	
}

class HistoryService[ORIGIN <: Node, E <: Edge, DEST <: Node]
(val relation: Relation[ORIGIN, E, DEST])
(implicit ignite: Ignite)
	extends HistorySource[ORIGIN, E, DEST] {
	
	val isInverse: Boolean = relation.isInverse
	val historyCacheName: String = HistoryUtil.getHistoryCacheNameFor(relation)
	val cache: IgniteCache[String, HistoryEntry] = ignite.getOrCreateCache(HistoryUtil.getHistoryCacheConfigFor(relation))
	
	def exists(originId: Id[ORIGIN], destinationId: Id[DEST]): Boolean = {
		isDefined(originId.id, destinationId.id)
	}
	
	private def isDefined(originId: String, destinationId: String): Boolean = {
		cache.containsKey(HistoryUtil.getHistoryKeyFor(originId, destinationId, isInverse))
	}
	
	def getDestinationIdsFor(originId: Id[ORIGIN], maxCount: Int = Int.MaxValue): Iterator[Id[DEST]] = {
		getObjIdIteratorFor[DEST](iterateOverObj1 = false, originId.id, maxCount)
	}
	
	def getOriginIdsFor(destinationId: Id[DEST], maxCount: Int = Int.MaxValue): Iterator[Id[ORIGIN]] = {
		getObjIdIteratorFor[ORIGIN](iterateOverObj1 = true, destinationId.id, maxCount)
	}
	
	private def getObjIdIteratorFor[TID <: Node]
	(iterateOverObj1: Boolean,
	 otherTypeObjId: String,
	 maxCount: Int): Iterator[Id[TID]] = {
		
		val useObj1 =
			if(isInverse)
				!iterateOverObj1
			else
				iterateOverObj1
		
		val queryFields =
			if(useObj1)
				(HistoryEntry.ORIGIN_ID_FIELD, HistoryEntry.DESTINATION_ID_FIELD)
			else
				(HistoryEntry.DESTINATION_ID_FIELD, HistoryEntry.ORIGIN_ID_FIELD)
		
		val query = new SqlFieldsQuery(
			"SELECT " + queryFields._1 +
				" FROM " + classOf[HistoryEntry].getSimpleName +
				" WHERE " + queryFields._2 + " = ?")
		query.setArgs(otherTypeObjId)
		query.setPageSize(maxCount)
		
		val iterWrapper = new CappedIterator(cache.query(query).iterator(), maxCount)
		new IdIterator(iterWrapper)
	}
	
	def removeByOriginIdAsync(originId: Id[ORIGIN], returnLatch: Boolean = false): Option[BatchLatch] = {
		removeAllOfIdAndTypeAsync(originId.id, isObj1Type = true, returnLatch)
	}
	
	def removeByDestinationIdAsync(destinationId: Id[DEST], returnLatch: Boolean = false): Option[BatchLatch] = {
		removeAllOfIdAndTypeAsync(destinationId.id, isObj1Type = false, returnLatch)
	}
	
	//returns list of other typed Ids that were removed
	private def removeAllOfIdAndTypeAsync
	(objId: String,
	 isObj1Type: Boolean,
	 returnLatch: Boolean): Option[BatchLatch] = {
		
		val useObj1 =
			if(isInverse)
				!isObj1Type
			else
				isObj1Type
		
		val objField =
			if(useObj1)
				HistoryEntry.ORIGIN_ID_FIELD
			else
				HistoryEntry.DESTINATION_ID_FIELD
		
		val query = new SqlFieldsQuery(
			"SELECT _key" +
				" FROM " + classOf[HistoryEntry].getSimpleName +
				" WHERE " + objField + " = ?")
		query.setArgs(objId)
		
		val keyIter = new CappedIterator(cache.query(query).iterator(), Int.MaxValue)
		val latch = BatchLatchFactory.newOptional(returnLatch)
		keyIter.foreach(key => {
			val future = cache.removeAsync(key)
			latch.foreach(l => l.listenForFuture(future))
		})
		
		latch
	}
	
	def putHistoryAsync(originId: Id[ORIGIN], destinationId: Id[DEST]): IgniteFuture[_] = {
		val key = HistoryUtil.getHistoryKeyFor(originId.id, destinationId.id, isInverse)
		val value = HistoryUtil.newHistoryFor(originId.id, destinationId.id, isInverse)
		cache.putAsync(key, value)
	}
	
	override def getOriginIdFrom(entry: HistoryEntry): Id[ORIGIN] =
		Id(
			if(isInverse) entry.getDestinationId
			else entry.getOriginId
		)
	
	override def getDestinationIdFrom(entry: HistoryEntry): Id[DEST] =
		Id(
			if(isInverse) entry.getOriginId
			else entry.getDestinationId
		)
	
}
