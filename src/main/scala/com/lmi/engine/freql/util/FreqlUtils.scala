package com.lmi.engine.freql.util

import com.lmi.engine.freql.score.{Score, TopScores}
import com.lmi.engine.graph.{Id, Node}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object FreqlUtils {
	
	
	def trimScores[OUTPUT <: Node](scoreMap: mutable.HashMap[Id[OUTPUT], Score[OUTPUT]],
	                               maxCount: Int): List[Score[OUTPUT]] = {
		val sorted = scoreMap.values.toList.sortWith((t1, t2) => {
			t1.points > t2.points
		})
		
		sorted.dropRight(Math.max(0, sorted.size - maxCount))
	}
	
	def normalize[INPUT <: Node, OUTPUT <: Node]
	(source: TopScores[INPUT, OUTPUT]): TopScores[INPUT, OUTPUT] = {
		val scores = source.scores
		var sum = 0.0
		
		scores.foreach(score => {
			sum += score.points
		})
		
		sum = Math.max(sum, Double.MinPositiveValue)
		
		val normalized = new ArrayBuffer[Score[OUTPUT]](scores.size)
		
		scores.foreach(score => {
			normalized += Score(score.similarTargetId, score.points / sum)
		})
		
		TopScores[INPUT, OUTPUT](source.originId, normalized)
	}
	
}
