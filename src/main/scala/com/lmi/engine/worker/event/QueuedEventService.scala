package com.lmi.engine.worker.event

import java.util.concurrent.atomic.AtomicLong

import com.lmi.engine.worker.parse.ParsedEvent
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.{Ignite, IgniteCache}
import org.apache.log4j.Logger

object QueuedEventIgniteUtil {
	
	val curId = new AtomicLong(0)
	private val QUEUE_CACHE_NAME = "QueuedEvents"
	private val logger = Logger.getLogger("KafkaEventStream")
	
	def createNextKey(ignite: Ignite): String = {
		if(curId.get() % 5000 == 0) {
			logger.info("Locally indexed " + curId.get())
			logger.info("In cache: " + getCache(ignite).size())
		}
		curId.incrementAndGet().toString + "," + ignite.cluster().localNode().id().toString
	}
	
	def getCache(ignite: Ignite): IgniteCache[String, ParsedEvent] =
		ignite.getOrCreateCache(getCacheConfig)
	
	def getCacheConfig: CacheConfiguration[String, ParsedEvent] = {
		val cacheConfig = new CacheConfiguration[String, ParsedEvent]()
		cacheConfig.setName(QUEUE_CACHE_NAME)
		cacheConfig.setCacheMode(CacheMode.PARTITIONED)
		cacheConfig.setBackups(0)
		
		cacheConfig
	}
	
	def getCacheName(ignite: Ignite): String =
		getCache(ignite).getName
	
}

class QueuedEventService(ignite: Ignite) {
	
	private val cache: IgniteCache[String, ParsedEvent] =
		ignite.getOrCreateCache(QueuedEventIgniteUtil.getCacheConfig)
	
	def save(key: String, qe: ParsedEvent): Unit = {
		cache.put(key, qe)
	}
	
}