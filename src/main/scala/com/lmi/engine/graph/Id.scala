package com.lmi.engine.graph

sealed case class Id[O <: Node](id: String) {
	
	override def equals(other: Any): Boolean = other match {
		case that: Id[O] =>
			(that canEqual this) &&
				id == that.id
		case _ => false
	}
	
	def canEqual(other: Any): Boolean = other.isInstanceOf[Id[O]]
	
	override def hashCode(): Int = {
		val state = Seq(id)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
}