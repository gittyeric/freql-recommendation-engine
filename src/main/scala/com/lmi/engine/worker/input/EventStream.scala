package com.lmi.engine.worker.input

import org.apache.ignite.Ignite

trait EventStream {
	
	def startStreaming(ignite: Ignite, cache: String): Unit
	
}
