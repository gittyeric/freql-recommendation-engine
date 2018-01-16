package com.lmi.engine.worker.output.trigger

import com.lmi.engine.freql.FreqlSelect
import com.lmi.engine.graph.{Edge, Id, Node}
import com.lmi.engine.worker.output.trigger.TriggeredResponseHelper._
import com.lmi.engine.worker.output.trigger.protocols.TriggerProtocol


final case class TriggeredQuery[INPUT <: Node, TObj <: Node, TRIGGER <: TriggerProtocol]
(query: FreqlSelect[INPUT, _ <: Edge, TObj, _ <: Node], protocol: TRIGGER) {
	
	def respond(request: TriggeredRequest): TriggeredResponse = {
		query.find(Id(request.id), request.limit)
	}
	
}
