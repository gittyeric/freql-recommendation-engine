package com.lmi.engine.freql.pivot

import com.lmi.engine.freql.score.Score
import com.lmi.engine.graph.Node

trait PivotOp[OUTPUT <: Node] {
	
	def pivot(existingTargetScore: Option[Score[OUTPUT]], existingCount: Int, newTargetScore: Score[OUTPUT]): Score[OUTPUT]
	
}
