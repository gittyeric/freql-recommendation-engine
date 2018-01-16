package com.lmi.engine.worker.util.ignite

import java.util.concurrent.atomic.AtomicInteger

sealed class CappedIterator(innerIter: java.util.Iterator[java.util.List[_]], cap: Int) extends Iterator[String] {
	
	val count = new AtomicInteger(0)
	
	def hasNext(): Boolean = {
		count.get() < cap && innerIter.hasNext
	}
	
	def next(): String = {
		val curCount = count.incrementAndGet()
		require(curCount < cap,
			"Called next() when there is no next")
		
		val nextRawId = innerIter.next()
		nextRawId.get(0) match {
			case originId: String => originId
			case _ => new String()
		}
	}
	
}
