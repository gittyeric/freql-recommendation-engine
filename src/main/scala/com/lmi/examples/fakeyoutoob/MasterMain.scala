package com.lmi.examples.fakeyoutoob

import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.util.ignite.IgniteConfig
import com.lmi.engine.{BasicProductionTuning, Engine}

object MasterMain {
	
	//JVM Entrypoint
	def main(args: Array[String]): Unit = {
		Engine.startMaster(
			createApp(),
			BasicProductionTuning(),
			() => IgniteConfig.getOrCreateIgnite())
	}
	
	def createApp(): FakeYouToobApp = {
		val inputStreams = Seq[EventStream]()
		new FakeYouToobApp(inputStreams)
	}
	
}
