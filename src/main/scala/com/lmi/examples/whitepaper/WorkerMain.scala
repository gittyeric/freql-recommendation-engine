package com.lmi.examples.whitepaper

import com.lmi.engine.{APIConfig, BasicProductionTuning, Engine}

object WorkerMain {
	
	//JVM Entrypoint
	def main(args: Array[String]): Unit = {
		val app = WhitepaperApp.createApp()
		
		Engine.startWorker(
			app,
			BasicProductionTuning(),
			() => Ignite.createConfiguredIgnite(),
			() => Some(new APIConfig(port = 8080))
		)
	}
	
}
