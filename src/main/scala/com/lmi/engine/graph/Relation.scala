package com.lmi.engine.graph

import com.lmi.engine.worker.util.Ids

case class Relation[ORIGIN <: Node, E <: Edge, DEST <: Node]
(origin: ORIGIN, edge: E, destination: DEST) {
	
	def isInverse: Boolean = edge.isInverse
	
	override def hashCode(): Int = {
		id.hashCode
	}
	
	override def equals(obj: scala.Any): Boolean = {
		obj match {
			case relation: Relation[_, _, _] =>
				id.equals(relation.id)
			case _ => false
		}
	}
	
	//A unique Id but ignores relationType direction,
	//i.e. Relation.bidirectionalId() == Relation.inverse().bidirectionalId()
	def bidirectionalId: String = {
		if(edge.isInverse)
			inverse().id
		else
			id
	}
	
	def id: String = {
		Ids.createKeyFrom(edge.name, origin.name, destination.name)
	}
	
	def inverse(): Relation[DEST, Inverse[E], ORIGIN] = {
		Relation(destination, edge.inverse(), origin)
	}
}
