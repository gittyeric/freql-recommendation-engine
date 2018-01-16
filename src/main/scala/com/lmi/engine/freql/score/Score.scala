package com.lmi.engine.freql.score

import com.lmi.engine.graph.{Id, Node}

case class Score[O <: Node](
	                           similarTargetId: Id[O],
	                           points: Double) extends Ordered[Score[O]] {
	
	override def hashCode(): Int = {
		val state = Seq(similarTargetId)
		state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
	}
	
	override def compare(that: Score[O]): Int = {
		-points.compare(that.points)
	}
	
	override def equals(other: Any): Boolean = other match {
		case that: Score[O] =>
			(that canEqual this) &&
				similarTargetId.equals(that.similarTargetId)
		case _ => false
	}
	
	def canEqual(other: Any): Boolean = other.isInstanceOf[Score[O]]
	
}
