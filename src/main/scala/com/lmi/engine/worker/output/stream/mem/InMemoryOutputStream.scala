package com.lmi.engine.worker.output.stream.mem

import com.lmi.engine.freql.score.TopScores
import com.lmi.engine.graph.{Id, Node}
import com.lmi.engine.worker.output.stream.OutputStream

import scala.collection.mutable

class InMemoryOutputStream[INPUT <: Node, TObj <: Node] extends OutputStream[INPUT, TObj] {
	
	val inputKeyToResults = new mutable.HashMap[Id[INPUT], TopScores[INPUT, TObj]]()
	val listeners = new mutable.HashMap[Id[INPUT],
		mutable.ListBuffer[Function[Id[INPUT], TopScores[INPUT, TObj]]]]()
	
	def getOutput(typedId: Id[INPUT]): Option[TopScores[INPUT, TObj]] = {
		inputKeyToResults.get(typedId)
	}
	
	def saveOutputAsync(scores: TopScores[INPUT, TObj]): Unit = {
		inputKeyToResults(scores.originId) = scores
	}
	
	def listen(targetToWaitFor: Id[INPUT], listener: Function[Id[INPUT], TopScores[INPUT, TObj]]): Unit = {
		val listenList = listeners.getOrElse(targetToWaitFor, new mutable.ListBuffer)
		listenList += listener
		listeners.put(targetToWaitFor, listenList)
	}
	
	def unlisten(targetToWaitFor: Id[INPUT], toRemove: Function[Id[INPUT], TopScores[INPUT, TObj]]): Unit = {
		val listenList = listeners.getOrElse(targetToWaitFor, new mutable.ListBuffer)
		listenList -= toRemove
	}
	
}
