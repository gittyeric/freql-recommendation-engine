package com.lmi.engine.freql.map.filter

import com.lmi.engine.freql.score.Score
import com.lmi.engine.graph.{Id, Node}

class AndOp
(f1: FilterOp, f2: FilterOp) extends FilterOp {
	
	def keep[INPUT <: Node, O <: Node](typedId: Id[INPUT], ts: Score[O]): Boolean = {
		f1.keep(typedId, ts) && f2.keep(typedId, ts)
	}
	
}

class OrOp
(f1: FilterOp, f2: FilterOp) extends FilterOp {
	
	def keep[INPUT <: Node, O <: Node](typedId: Id[INPUT], ts: Score[O]): Boolean = {
		f1.keep(typedId, ts) || f2.keep(typedId, ts)
	}
	
}

case class Not
(f1: FilterOp) extends FilterOp {
	
	def keep[INPUT <: Node, O <: Node](typedId: Id[INPUT], ts: Score[O]): Boolean = {
		!f1.keep(typedId, ts)
	}
	
}