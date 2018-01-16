package com.lmi.engine.freql.map

import com.lmi.engine.freql.score.{TopScoreSource, TopScores}
import com.lmi.engine.freql.util.FreqlUtils
import com.lmi.engine.graph.{Edge, Id, Node}

class SourceMapper[INPUT <: Node, T <: Edge, TObj <: Node, GIVEN <: Node]
(source: TopScoreSource[INPUT, T, TObj, GIVEN], op: MapOp)
	extends TopScoreSource[INPUT, T, TObj, GIVEN] {
	
	override def getRecommendations(originId: Id[INPUT], maxCount: Int): TopScores[INPUT, TObj] = {
		val input = source.getRecommendations(originId, maxCount)
		val mapped = op.apply(input)
		
		FreqlUtils.normalize(mapped)
	}
}
