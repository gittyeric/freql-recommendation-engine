package com.lmi.engine

import com.lmi.engine.graph.{Edge, Node}
import com.lmi.engine.worker.RelationService
import com.lmi.engine.worker.event.EventRouter
import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.output.trigger.TriggeredQuery
import com.lmi.engine.worker.output.trigger.protocols.TriggerProtocol

trait FreqlApp extends Serializable {
	
	def name: String
	
	//Define how to map input events to Historic Relations
	def inputEvents: EventInputs
	
	//Map queries to outputs
	def outputs: Outputs
	
	//Define computation pipelines
	def pipelines(): Seq[RelationService[_ <: Node, _ <: Edge, _ <: Node]]
	
	//Defines max number of most recent (LRU) nodes of a type will be stored for any relations
	//No LRU eviction will occur for Long.MaxValue
	def maxObjCount(objType: Node): Int = Int.MaxValue
	
}

final case class EventInputs(
	                             inputs: Seq[EventStream],
	                             router: EventRouter
                            ) extends Serializable

final case class Outputs(triggeredOutputs: Seq[TriggeredQuery[_ <: Node, _ <: Node, _ <: TriggerProtocol]] = Seq())
	extends Serializable
