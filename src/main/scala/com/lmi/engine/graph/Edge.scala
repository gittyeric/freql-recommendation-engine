package com.lmi.engine.graph

trait Edge {
	
	override def equals(obj: scala.Any): Boolean = {
		obj match {
			case e: Edge => name.equals(e.name)
			case _ => false
		}
	}
	
	def name: String = {
		getClass.getSimpleName
	}
	
	def isInverse: Boolean = false
	
	def inverse[THIS <: Edge](): Inverse[THIS] = new Inverse(this.asInstanceOf[THIS])
	
}

//Used by services to de-duplicate data storage when both
//a relation and it's inverse relation is needed
class Inverse[E <: Edge](private val innerEdge: E) extends Edge {
	
	//Need to essentially force Inverse[Inverse[E]].name == E.name
	override def name: String = trueName(innerEdge, true)
	
	//Need to essentially force Inverse[Inverse[E]].name == false
	override def isInverse: Boolean = isInverseRecursive(innerEdge, true)
	
	private def trueName(nestedEdge: Edge, isInverse: Boolean): String = {
		nestedEdge match {
			case e: Inverse[_] =>
				e.trueName(e.innerEdge, !isInverse)
			case _ =>
				if(isInverse)
					"Inverse[" + nestedEdge.name + "]"
				else
					nestedEdge.name
		}
	}
	
	private def isInverseRecursive(e: Edge, wasInverse: Boolean): Boolean = {
		e match {
			case i: Inverse[_] => i.isInverseRecursive(i.innerEdge, !wasInverse)
			case _ => wasInverse
		}
	}
	
}