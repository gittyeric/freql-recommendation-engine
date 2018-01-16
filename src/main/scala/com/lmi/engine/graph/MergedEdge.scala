package com.lmi.engine.graph

class MergedEdge[E1 <: Edge, E2 <: Edge](r1: E1, r2: E2) extends Edge {
	
	override def name: String = {
		r1.name + "::" + r2.name
	}
	
}
