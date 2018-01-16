package com.lmi.engine.freql.pivot

import com.lmi.engine.freql.score.{Score, TopScoreSource, TopScores}
import com.lmi.engine.freql.util.FreqlUtils
import com.lmi.engine.graph._

import scala.collection.mutable

class SourcesPivoter[INPUT <: Node, T <: Edge, T2 <: Edge, PivotOBJ <: Node, GIVEN <: Node, GIVEN2 <: Node, OUTPUT <: Node]
(source1: TopScoreSource[INPUT, T, PivotOBJ, GIVEN],
 source2: TopScoreSource[PivotOBJ, T2, OUTPUT, GIVEN2],
 op: PivotOp[OUTPUT],
 maxSource2Targets: Int = Int.MaxValue)
	extends TopScoreSource[INPUT, MergedEdge[T, T2], OUTPUT, MergedNode[GIVEN, GIVEN2]] {
	
	override def getRecommendations(originId: Id[INPUT], maxCount: Int): TopScores[INPUT, OUTPUT] = {
		val targetToScores = new mutable.HashMap[Id[OUTPUT], Score[OUTPUT]]()
		val targetToSeenCount = new mutable.HashMap[Id[OUTPUT], Int]()
		
		val scores = source1.getRecommendations(originId, maxCount)
		
		scores.scores.foreach(ts => {
			val scoresForTarget = source2.getRecommendations(ts.similarTargetId, maxSource2Targets)
			
			scoresForTarget.scores.foreach(innerScore => {
				val existingScore = targetToScores.get(innerScore.similarTargetId)
				val seenCount = targetToSeenCount.getOrElse(innerScore.similarTargetId, 0)
				val newScore = op.pivot(existingScore, seenCount, innerScore)
				
				targetToSeenCount.put(innerScore.similarTargetId, seenCount + 1)
				targetToScores.put(innerScore.similarTargetId, newScore)
			})
			
		})
		
		val pivoted = FreqlUtils.trimScores(targetToScores, maxCount)
		FreqlUtils.normalize(TopScores[INPUT, OUTPUT](scores.originId, pivoted))
	}
}
