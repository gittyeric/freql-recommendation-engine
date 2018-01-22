package com.lmi.engine.worker.history

import com.lmi.engine.graph.{Edge, Node}

trait HistoricProvider[ORIGIN <: Node, E <: Edge, DEST <: Node] {
	
	def items: HistorySource[ORIGIN, E, DEST]
	
}
