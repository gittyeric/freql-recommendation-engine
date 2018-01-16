package com.lmi.engine.worker.output.trigger.protocols

import com.lmi.engine.freql.FreqlSelect
import com.lmi.engine.graph.{Edge, Node}
import com.lmi.engine.worker.output.trigger.TriggeredQuery

object TriggeredMem {
	
	def MemQuery[INPUT <: Node, TObj <: Node]
	(query: FreqlSelect[INPUT, _ <: Edge, TObj, _ <: Node]): TriggeredQuery[INPUT, TObj, MemProtocol] = {
		new TriggeredQuery(query, new MemProtocol)
	}
	
}

sealed case class MemProtocol() extends TriggerProtocol {}
