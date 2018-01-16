package com.lmi.engine

import com.lmi.engine.graph.Node
import com.lmi.engine.worker.event.EventRouter
import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.output.trigger.TriggeredQuery
import com.lmi.engine.worker.{PipelineContext, RecommendPipeline}

trait FreqlApp extends Serializable {
	
	def name: String
	
	//Define how to map input events to Historic Relations
	def eventSources: EventSources
	
	//Define computation pipelines to their outputs
	def buildComputationGraph()(implicit context: PipelineContext): ComputationGraph
	
	//Defines max number of most recent (LRU) nodes of a type will be stored for any relations
	//No LRU eviction will occur for Long.MaxValue
	def maxObjCount(objType: Node): Int = Int.MaxValue
	
}

final case class EventSources(
	                             inputs: Seq[EventStream],
	                             router: EventRouter
                             )

final case class ComputationGraph(
	                                 pipelines: Seq[RecommendPipeline[_, _, _]],
	                                 triggeredOutputs: Seq[TriggeredQuery[_, _, _]] = Seq()
                                 )