package com.lmi.engine.worker.input

import org.apache.ignite.Ignite

trait EventStream extends Serializable {
	
	def startStreaming(ignite: Ignite, cacheName: String): Unit
	
}
