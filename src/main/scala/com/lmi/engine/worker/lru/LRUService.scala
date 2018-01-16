package com.lmi.engine.worker.lru

import java.util
import javax.cache.Cache

import com.lmi.engine.graph.{Id, Node}
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicy
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.lang.IgniteFuture
import org.apache.ignite.{Ignite, IgniteCache}

object LRUUtil {
	
	def getLRUCacheConfigFor(obj: Node, maxItems: Int): CacheConfiguration[String, Boolean] = {
		val cacheConfig = new CacheConfiguration[String, Boolean]()
		cacheConfig.setName(getCacheName(obj))
		cacheConfig.setCacheMode(CacheMode.PARTITIONED)
		cacheConfig.setBackups(0)
		cacheConfig.setOnheapCacheEnabled(true)
		cacheConfig.setEvictionPolicy(new LruEvictionPolicy[String, Int](maxItems))
		
		cacheConfig
	}
	
	def getCacheName(obj: Node): String = {
		"LRU_" + obj.name
	}
	
}

class LRUService[O <: Node]
(val obj: O, val maxItems: Int)
(implicit ignite: Ignite) {
	
	private val cache: IgniteCache[String, Boolean] =
		ignite.getOrCreateCache(LRUUtil.getLRUCacheConfigFor(obj, maxItems))
	
	//Accesses this to reset it's TTL eviction time
	def touchAsync(key: Id[O]): IgniteFuture[_] = {
		cache.putAsync(key.id, true)
	}
	
	def iterator(): util.Iterator[Cache.Entry[String, Boolean]] = {
		cache.iterator()
	}
	
	def cacheName(): String = {
		LRUUtil.getCacheName(obj)
	}
	
}
