package com.lmi.engine.graph

trait Node {
	
	def name: String = {
		getClass.getSimpleName
	}
	
}
