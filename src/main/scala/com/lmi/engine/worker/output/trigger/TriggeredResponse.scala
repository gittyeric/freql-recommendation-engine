package com.lmi.engine.worker.output.trigger

import com.lmi.engine.freql.score.TopScores
import com.lmi.engine.graph.Node

object TriggeredResponseHelper {
	
	implicit def topScoresToResponse(scores: TopScores[_ <: Node, _ <: Node]): TriggeredResponse = {
		val respScores = scores.scores.map(score =>
			TriggeredResponseScore(score.points, score.similarTargetId.id))
		
		TriggeredResponse(respScores)
	}
	
}

final case class TriggeredResponse(scores: Seq[TriggeredResponseScore]) {}

final case class TriggeredResponseScore(points: Double, id: String) {}