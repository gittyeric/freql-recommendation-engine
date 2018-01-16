package com.lmi.engine.worker.event

import com.lmi.engine.graph.{Id, Node}

trait HistoricEvent[EVENT <: EventType, ORIGIN <: Node, DEST <: Node] {
	
	def eventType: EVENT
	
	def originId(): Id[ORIGIN]
	
	def destinationId(): Id[DEST]
	
}
