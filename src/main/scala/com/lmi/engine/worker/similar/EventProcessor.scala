package com.lmi.engine.worker.similar

import com.lmi.engine.worker.parse.ParsedEvent

//Basic observer for incoming ParsedEvents
abstract class EventProcessor(val eventType: String) {
	
	def process(event: ParsedEvent): Unit
	
	def canProcess(event: ParsedEvent): Boolean = {
		event.eventType.equals(eventType)
	}
	
}
