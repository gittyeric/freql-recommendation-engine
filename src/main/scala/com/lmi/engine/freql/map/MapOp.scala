package com.lmi.engine.freql.map

import com.lmi.engine.freql.score.TopScores
import com.lmi.engine.graph.Node

trait MapOp {
	
	def apply[INPUT <: Node, O <: Node](topScores: TopScores[INPUT, O]): TopScores[INPUT, O]
	
}
