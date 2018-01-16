package com.lmi.engine.worker.util.ignite

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.ignite.Ignite
import org.apache.ignite.events.CacheEvent
import org.apache.ignite.lang.IgnitePredicate

import scala.collection.mutable.ListBuffer

object IgniteEventUtil {
	
	val listening = new AtomicBoolean(false)
	val typeToPipes = new ConcurrentHashMap[Int, IgniteEventPipe]()
	
	def getPipe(igniteEvent: Int)
	           (implicit ignite: Ignite): IgniteEventPipe = {
		ensureListening()
		
		typeToPipes.putIfAbsent(igniteEvent, new IgniteEventPipe(igniteEvent))
		typeToPipes.get(igniteEvent)
	}
	
	private def ensureListening()(implicit ignite: Ignite): Unit = {
		val wasListening = listening.getAndSet(true)
		
		if(!wasListening) {
			ignite.events().localListen(
				new IgnitePredicate[CacheEvent] {
					override def apply(event: CacheEvent): Boolean = {
						val pipe = typeToPipes.get(event.`type`())
						if(pipe != null) {
							val listeners = pipe.cacheListeners.get(event.cacheName())
							if(listeners != null) {
								listeners.foreach(_.apply(event))
							}
						}
						true
					}
				}, IgniteConfig.FREQL_IGNITE_EVENT_TYPES: _*)
		}
	}
	
}

sealed class IgniteEventPipe(igniteEventType: Int) {
	
	val cacheListeners = new ConcurrentHashMap[String, ListBuffer[(CacheEvent) => Unit]]()
	
	def listen(cacheName: String, listener: ((CacheEvent) => Unit)): Unit = {
		cacheListeners.putIfAbsent(cacheName, new ListBuffer[(CacheEvent) => Unit])
		cacheListeners.get(cacheName) += listener
	}
	
}