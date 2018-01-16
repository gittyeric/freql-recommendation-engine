package com.lmi.engine.graph

class IdIterator[O <: Node](iter: Iterator[String]) extends Iterator[Id[O]] {
	
	override def hasNext: Boolean =
		iter.hasNext
	
	override def next(): Id[O] =
		Id(iter.next())
}
