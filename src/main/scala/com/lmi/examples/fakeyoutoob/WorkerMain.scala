package com.lmi.examples.fakeyoutoob

import com.lmi.engine.worker.util.ignite.IgniteConfig
import com.lmi.engine.{BasicProductionTuning, Engine}
import com.lmi.examples.fakeyoutoob.MasterMain.createApp

class WorkerMain {
	
	//JVM Entrypoint
	def main(args: Array[String]): Unit = {
		Engine.startWorker(
			createApp(),
			BasicProductionTuning(),
			() => IgniteConfig.getOrCreateIgnite())
	}
	
}
