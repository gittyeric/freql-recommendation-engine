package com.lmi.engine.freql.traverse

import com.lmi.engine.freql.score.Score
import com.lmi.engine.graph.Node

trait TraverseOp[O <: Node] {
	
	def update(existingScore: Option[Score[O]], newScore: Score[O]): Score[O]
	
}
