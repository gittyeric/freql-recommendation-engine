package com.lmi.examples.whitepaper

import com.lmi.engine.{APIConfig, BasicProductionTuning, Engine}

object MasterMain {
	
	//JVM Entrypoint
	def main(args: Array[String]): Unit = {
		//Replace with YOUR App instance
		val app = WhitepaperApp.createApp()
		
		Engine.startMaster(
			app,
			BasicProductionTuning(),
			() => Ignite.createConfiguredIgnite(),
			() => Some(new APIConfig(port = 8080))
		)
	}
	
	
}
