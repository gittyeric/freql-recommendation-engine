package com.lmi.engine.freql.pivot

import com.lmi.engine.freql.score.Score
import com.lmi.engine.graph.Node

//Takes some set of inputs and finds targets of the same type that are the most
//similar to the input set, thus generalizing the input to more generally popular targets
class WeightedPipeOp[OUTPUT <: Node] extends PivotOp[OUTPUT] {
	
	def pivot(existingTargetScore: Option[Score[OUTPUT]], existingCount: Int, newTargetScore: Score[OUTPUT]): Score[OUTPUT] = {
		val newCount = existingCount + 1
		val existingScore = if(existingTargetScore.isDefined) existingTargetScore.get.points else 0.0
		Score[OUTPUT](newTargetScore.similarTargetId, (existingScore * existingCount + newTargetScore.points) / newCount)
	}
	
}
