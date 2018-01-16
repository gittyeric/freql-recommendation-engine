package com.lmi.examples.whitepaper

import com.lmi.engine.ComputationGraph
import com.lmi.engine.worker.PipelineContext
import com.lmi.engine.worker.input.ManualEventStream
import com.lmi.engine.worker.output.trigger.protocols.TriggeredMem.MemQuery

class TestWhitepaperApp() extends WhitepaperApp(Seq(new ManualEventStream)) {
	
	override def buildComputationGraph()(implicit context: PipelineContext): ComputationGraph = {
		val superGraph = super.buildComputationGraph()
		val testOutputs = Seq(
			
			//Offer recommendations through HTTP interface
			MemQuery(instance.papersByAuthors),
			MemQuery(instance.papersByCat),
			MemQuery(instance.papersByReference),
			MemQuery(instance.papersByTag),
			MemQuery(instance.papersByAll),
			
			MemQuery(instance.filesByAll),
			MemQuery(instance.catsByAll),
			MemQuery(instance.tagsByAll),
			MemQuery(instance.groupsByAll),
			MemQuery(instance.authorsByAll)
		)
		
		return new ComputationGraph(superGraph.pipelines, testOutputs)
	}
	
	class TestWhitepaperAppInstance(implicit context: PipelineContext) extends WhitepaperAppInstance {
	
	
	}
	
}
