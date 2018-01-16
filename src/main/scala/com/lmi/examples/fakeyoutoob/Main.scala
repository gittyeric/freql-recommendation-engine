package com.lmi.examples.fakeyoutoob

import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.util.ignite.IgniteConfig
import com.lmi.engine.{BasicProductionTuning, Engine}

object Main {
	
	//JVM Entrypoint
	def main(args: Array[String]): Unit = {
		//Replace with YOUR App instance
		val inputStreams = Seq[EventStream]()
		val app = new FakeYouToobApp(inputStreams)
		
		Engine.start(
			app,
			BasicProductionTuning(),
			() => IgniteConfig.getOrCreateIgnite())
	}
	
}
