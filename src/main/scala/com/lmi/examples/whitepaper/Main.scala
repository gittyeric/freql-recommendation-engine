package com.lmi.examples.whitepaper

import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.input.kafka.KafkaEventStream
import com.lmi.engine.worker.parse.DelimiterParser
import com.lmi.engine.worker.util.ignite.IgniteConfig
import com.lmi.engine.{APIConfig, BasicProductionTuning, Engine}

object Main {
	
	//JVM Entrypoint
	def main(args: Array[String]): Unit = {
		//Replace with YOUR App instance
		val tsvParser = new DelimiterParser('\t')
		val inputStreams = Seq[EventStream](new KafkaEventStream("events", tsvParser.parseEvent, "192.241.235.113:2181"))
		val app = new WhitepaperApp(inputStreams)
		
		Engine.start(
			app,
			BasicProductionTuning(),
			() => IgniteConfig.getOrCreateIgnite(),
			() => Some(new APIConfig(port = 8080))
		)
	}
	
}
