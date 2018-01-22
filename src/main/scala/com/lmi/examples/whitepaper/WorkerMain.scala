package com.lmi.examples.whitepaper

import com.lmi.engine.worker.util.ignite.IgniteConfig
import com.lmi.engine.{APIConfig, BasicProductionTuning, Engine}

object WorkerMain {
	
	//JVM Entrypoint
	def main(args: Array[String]): Unit = {
		val app = MasterMain.createApp()
		
		Engine.startWorker(
			app,
			BasicProductionTuning(),
			() => IgniteConfig.getOrCreateIgnite(),
			() => Some(new APIConfig(port = 8080))
		)
	}
	
}
