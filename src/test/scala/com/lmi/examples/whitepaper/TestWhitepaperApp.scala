package com.lmi.examples.whitepaper

import com.lmi.engine.Outputs
import com.lmi.engine.worker.input.ManualEventStream
import com.lmi.engine.worker.output.trigger.protocols.TriggeredMem.MemQuery

class TestWhitepaperApp() extends WhitepaperApp(Seq(new ManualEventStream)) {
	
	override def outputs(): Outputs = {
		val testOutputs = Seq(
			
			//Offer recommendations through HTTP interface
			MemQuery(papersByAuthors),
			MemQuery(papersByCat),
			MemQuery(papersByReference),
			MemQuery(papersByTag),
			MemQuery(papersByAll),
			
			MemQuery(filesByAll),
			MemQuery(catsByAll),
			MemQuery(tagsByAll),
			MemQuery(groupsByAll),
			MemQuery(authorsByAll)
		)
		
		return new Outputs(testOutputs)
	}
	
}
