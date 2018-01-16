package com.lmi.engine.freql.join

import com.lmi.engine.freql.score.{Score, TopScores}
import com.lmi.engine.graph.{Id, Node}

import scala.collection.mutable

trait JoinByKeyOp {
	
	def join[INPUT <: Node, O <: Node](topScoresA: TopScores[INPUT, O], topScoresB: TopScores[INPUT, O]): TopScores[INPUT, O] = {
		var originId: Id[INPUT] = null
		val joined = new mutable.LinkedHashMap[Id[O], Double]
		
		Seq(topScoresA, topScoresB).foreach(scores => {
			originId = scores.originId
			
			scores.scores.foreach(score => {
				val current = joined.get(score.similarTargetId)
				val newVal = join(current, score.points)
				
				if(newVal.isDefined) {
					joined.put(score.similarTargetId, newVal.get)
				}
				else {
					joined.remove(score.similarTargetId)
				}
			})
		})
		
		val joinedScores = joined.keys.toSeq.map(
			(similarTargetId) => {
				Score[O](similarTargetId, joined(similarTargetId))
			}
		)
		
		joinedScores.sortWith((score1, score2) =>
			score1.points > score2.points)
		
		TopScores[INPUT, O](originId, joinedScores)
	}
	
	protected def join(oldScore: Option[Double], score: Double): Option[Double]
	
}
