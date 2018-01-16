package com.lmi.engine.freql.map.filter

import com.lmi.engine.freql.map.MapOp
import com.lmi.engine.freql.score.{Score, TopScores}
import com.lmi.engine.graph.{Id, Node}

trait FilterOp extends MapOp {
	
	def keep[INPUT <: Node, O <: Node](typedId: Id[INPUT], ts: Score[O]): Boolean
	
	def apply[INPUT <: Node, O <: Node](topScores: TopScores[INPUT, O]): TopScores[INPUT, O] = {
		val filtered = topScores.scores.filterNot(keep(topScores.originId, _))
		TopScores(topScores.originId, filtered)
	}
	
	def And[INPUT <: Node, O <: Node](f: FilterOp): FilterOp = {
		new AndOp(this, f)
	}
	
	def Or[INPUT <: Node, O <: Node](f: FilterOp): FilterOp = {
		new OrOp(this, f)
	}
	
}
