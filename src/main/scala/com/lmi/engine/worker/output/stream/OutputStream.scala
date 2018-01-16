package com.lmi.engine.worker.output.stream

import com.lmi.engine.freql.score.TopScores
import com.lmi.engine.graph.Node

trait OutputStream[INPUT <: Node, TObj <: Node] {
	
	def saveOutputAsync(scores: TopScores[INPUT, TObj]): Unit
	
}
