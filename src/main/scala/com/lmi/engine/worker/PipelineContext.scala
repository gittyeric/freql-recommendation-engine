package com.lmi.engine.worker

import com.lmi.engine.EngineTuning
import com.lmi.engine.graph.Node
import org.apache.ignite.Ignite

trait PipelineContext {
	
	def tuning: EngineTuning
	
	def ignite: Ignite
	
	def maxObjCount(objType: Node): Int
	
}
