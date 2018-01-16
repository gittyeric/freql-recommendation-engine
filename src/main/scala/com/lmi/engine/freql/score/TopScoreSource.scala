package com.lmi.engine.freql.score

import com.lmi.engine.graph.{Edge, Id, Node}

trait TopScoreSource[INPUT <: Node, T <: Edge, OUTPUT <: Node, GIVEN <: Node] {
	
	def getRecommendations(id: Id[INPUT], maxCount: Int): TopScores[INPUT, OUTPUT]
	
}
