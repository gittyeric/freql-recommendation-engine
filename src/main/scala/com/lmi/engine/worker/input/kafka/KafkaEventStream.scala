package com.lmi.engine.worker.input.kafka

import java.util.AbstractMap.SimpleEntry
import java.util.{Map, Properties}

import com.lmi.engine.EngineThreads
import com.lmi.engine.worker.event.QueuedEventIgniteUtil
import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.parse.ParsedEvent
import kafka.consumer.ConsumerConfig
import kafka.message.MessageAndMetadata
import org.apache.ignite.Ignite
import org.apache.ignite.stream.StreamSingleTupleExtractor
import org.apache.ignite.stream.kafka.KafkaStreamer
import org.apache.log4j.Logger

import scala.util.Try

class KafkaEventStream(val topic: String,
                       val lineToEvent: String => Try[ParsedEvent],
                       val zookeeper: String = "localhost:2181",
                       val groupId: String = "2", startingOffset: String = "smallest", timeoutMs: Int = 10000) extends EventStream {
	
	private val logger = Logger.getLogger("KafkaEventStream")
	
	override def startStreaming(ignite: Ignite, cacheName: String): Unit = {
		val kafkaStreamer = new KafkaStreamer[String, ParsedEvent]()
		
		kafkaStreamer.setSingleTupleExtractor(new StreamSingleTupleExtractor[MessageAndMetadata[Array[Byte], Array[Byte]], String, ParsedEvent] {
			override def extract(msg: MessageAndMetadata[Array[Byte], Array[Byte]]): Map.Entry[String, ParsedEvent] = {
				val line = new String(msg.message(), "UTF8")
				val value = lineToEvent(line)
				val key = QueuedEventIgniteUtil.createNextKey(ignite)
				
				if(value.isFailure) {
					//logger.error("Could not parse line: " + line)
				}
				
				new SimpleEntry(key, value.getOrElse(null))
			}
		})
		
		try {
			val consumerProps = new Properties()
			consumerProps.setProperty("zookeeper.connect", zookeeper)
			consumerProps.setProperty("group.id", groupId)
			consumerProps.setProperty("request.timeout.ms", timeoutMs.toString)
			consumerProps.setProperty("auto.offset.reset", startingOffset)
			
			val dataStreamer = ignite.dataStreamer[String, ParsedEvent](cacheName)
			dataStreamer.allowOverwrite(false)
			kafkaStreamer.setStreamer(dataStreamer)
			
			kafkaStreamer.setIgnite(ignite)
			kafkaStreamer.setTopic(topic)
			kafkaStreamer.setThreads(EngineThreads.THREAD_PRIORITY_HIGH)
			
			kafkaStreamer.setConsumerConfig(new ConsumerConfig(consumerProps))
			kafkaStreamer.start()
		}
		catch {
			case e: Exception =>
				throw new RuntimeException(e)
		}
	}
	
}
