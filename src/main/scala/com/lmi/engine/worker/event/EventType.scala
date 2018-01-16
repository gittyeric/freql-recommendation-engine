package com.lmi.engine.worker.event

abstract class EventType(val str: String) {
	
	override def toString: String = str
	
	override def hashCode(): Int = str.hashCode
	
	override def equals(obj: scala.Any): Boolean = str.equals(obj)
	
}
