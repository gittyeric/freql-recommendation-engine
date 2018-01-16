package com.lmi.engine.worker.event

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

import com.lmi.engine.worker.history.{HistoryEntry, HistoryUtil}
import com.lmi.engine.worker.parse.ParsedEvent
import com.lmi.engine.{EngineContext, EngineThreads}
import org.apache.ignite.IgniteCache

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class EventDequeuer(router: EventRouter)
                   (implicit context: EngineContext) {
	
	val tuning = context.tuning
	var isRunning = new AtomicBoolean(false)
	
	def ensureIsDequeuing(): Unit = {
		val wasRunning = isRunning.getAndSet(true)
		if(!wasRunning) {
			val cache: IgniteCache[String, ParsedEvent] = QueuedEventIgniteUtil.getCache(context.ignite)
			val historyCaches = new mutable.HashMap[String, IgniteCache[String, HistoryEntry]]()
			
			router.relations().foreach((relation) => {
				val cacheName = HistoryUtil.getHistoryCacheNameFor(relation)
				val historyCache: IgniteCache[String, HistoryEntry] = context.ignite.cache(cacheName)
				
				historyCaches.put(relation.id, historyCache)
				historyCaches.put(relation.inverse().id, historyCache)
			})
			
			val dequeueJob = new Runnable {
				override def run(): Unit = {
					val polled = popNextFromLocal(cache, tuning.MAX_DEQUEUES_PER_RUN)
					if(polled.isEmpty) {
						Thread.sleep(tuning.EMPTY_DEQUEUE_SLEEP_MS)
					}
					
					val histories = router.route(polled)
					histories.foreach((history) => {
						val historyCache = historyCaches(history._1.id)
						val entry = history._2
						val key = HistoryUtil.getHistoryKeyFor(entry.getOriginId, entry.getDestinationId, isInverse = false)
						
						historyCache.putIfAbsent(key, entry)
					})
				}
			}
			
			EngineThreads.dequeuePool.scheduleWithFixedDelay(dequeueJob, 1, 1, TimeUnit.MILLISECONDS)
		}
	}
	
	//Unfortunately this must be sync since ignite doesn't offer async+local access.
	//It's still faster to process locally in-sync than async over the network
	private def popNextFromLocal(cache: IgniteCache[String, ParsedEvent], count: Long): Seq[ParsedEvent] = {
		val popped = new ListBuffer[ParsedEvent]
		
		val nextIter = cache.localEntries().iterator()
		while (nextIter.hasNext && popped.length < count) {
			val nextEvent = nextIter.next()
			popped += nextEvent.getValue
			cache.remove(nextEvent.getKey)
		}
		
		popped
	}
	
}
