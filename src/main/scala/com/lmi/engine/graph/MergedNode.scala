package com.lmi.engine.graph

class MergedNode[Obj1 <: Node, Obj2 <: Node](
	                                            obj1: Obj1, obj2: Obj2) extends Node {
	
	override def name: String = {
		obj1.name + "::" + obj2.name
	}
	
}
