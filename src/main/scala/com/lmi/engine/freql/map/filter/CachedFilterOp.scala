package com.lmi.engine.freql.map.filter

import java.util.concurrent.TimeUnit

import com.google.common.cache.{Cache, CacheBuilder}
import com.lmi.engine.freql.score.Score
import com.lmi.engine.graph.{Id, Node}
import com.lmi.engine.worker.util.Ids

trait CachedFilterOp
	extends FilterOp {
	
	private val trueCache = CacheBuilder.newBuilder()
		.expireAfterAccess(trueCacheExpirationMs, TimeUnit.MILLISECONDS)
		.concurrencyLevel(4)
		.build()
		.asInstanceOf[Cache[String, Boolean]].asMap()
	
	private val falseCache = CacheBuilder.newBuilder()
		.expireAfterAccess(falseCacheExpirationMs, TimeUnit.MILLISECONDS)
		.concurrencyLevel(4)
		.build()
		.asInstanceOf[Cache[String, Boolean]].asMap()
	
	override final def keep[INPUT <: Node, O <: Node](typedId: Id[INPUT], ts: Score[O]): Boolean = {
		val cacheKey = getCacheKey(typedId, ts)
		if(trueCache.containsKey(typedId)) {
			true
		}
		else if(falseCache.containsKey(typedId)) {
			false
		}
		else {
			val shouldAllow = uncachedAllow(typedId, ts)
			if(shouldAllow) {
				trueCache.put(cacheKey, true)
			}
			else {
				falseCache.put(cacheKey, false)
			}
			
			shouldAllow
		}
	}
	
	protected def getCacheKey[INPUT <: Node, O <: Node](typedId: Id[INPUT], ts: Score[O]): String = {
		Ids.createKeyFrom(typedId.id, ts.similarTargetId.id)
	}
	
	def trueCacheExpirationMs: Int
	
	def falseCacheExpirationMs: Int
	
	def uncachedAllow[INPUT <: Node, O <: Node](typedId: Id[INPUT], ts: Score[O]): Boolean
	
}
