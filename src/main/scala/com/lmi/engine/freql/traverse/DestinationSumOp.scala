package com.lmi.engine.freql.traverse

import com.lmi.engine.freql.score.Score
import com.lmi.engine.graph.Node

case class DestinationSumOp[O <: Node]() extends TraverseOp[O] {
	
	def update(existingScore: Option[Score[O]], newScore: Score[O]): Score[O] = {
		val existing = if(existingScore.isDefined) existingScore.get.points else 0.0
		Score[O](newScore.similarTargetId, existing + newScore.points)
	}
	
}
