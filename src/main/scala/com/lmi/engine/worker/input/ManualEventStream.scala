package com.lmi.engine.worker.input

import java.util.concurrent.atomic.AtomicInteger

import com.lmi.engine.worker.event.QueuedEventService
import com.lmi.engine.worker.parse.ParsedEvent
import org.apache.ignite.Ignite

class ManualEventStream extends EventStream {
	
	private val counter = new AtomicInteger(1)
	private var eventService: Option[QueuedEventService] = None
	
	def add(event: ParsedEvent): Unit = {
		require(eventService.nonEmpty, "add called before startStreaming")
		eventService.get.save(counter.incrementAndGet().toString, event)
	}
	
	def startStreaming(ignite: Ignite, cache: String): Unit = {
		eventService = Some(new QueuedEventService(ignite))
	}
	
}
