package com.lmi.engine.freql.map

import com.lmi.engine.freql.score.{Score, TopScores}
import com.lmi.engine.graph.Node

import scala.collection.mutable.ListBuffer
import scala.util.Random

//fairness of 0.0 means completely random redistribution,
// fairness of 1.0 means to randomly distribute weighted by score
//maxDemotions caps number of positions a score can randomly drop from its input position
class DitherOp(fairness: Double, maxDemotions: Int, seed: Long) extends MapOp {
	
	val rand = new Random(seed)
	
	if(fairness < 0.0 || fairness > 1.0) {
		throw new RuntimeException("You need to use a valid fairness")
	}
	
	def apply[INPUT <: Node, O <: Node](topScores: TopScores[INPUT, O]): TopScores[INPUT, O] = {
		val unfairness = 1.0 - fairness
		val count = topScores.scores.size
		val unfair = 1.0 / count
		var pointsSum = 0.0
		val originId = topScores.originId
		
		topScores.scores.foreach(topScore => {
			pointsSum += topScore.points
		})
		
		var scoreIndex = 0
		val indexProbAndScores = topScores.scores.map(topScore => {
			val fair = topScore.points / pointsSum
			
			val probIntervalSize = unfair * unfairness + fair * fairness
			
			val toReturn = ((scoreIndex, probIntervalSize), topScore)
			scoreIndex += 1
			toReturn
		}).toBuffer
		
		val randomlySorted = new ListBuffer[Score[O]]
		
		for (newListIndex <- 0 to count) {
			val dice = rand.nextDouble() * pointsSum
			var curPointsIndex = 0.0
			var findIndex = 0
			
			val removed = indexProbAndScores.find(indexProbAndScore => {
				curPointsIndex += indexProbAndScore._1._2
				val originalIndex = indexProbAndScore._1._1
				val diceIndexFound = curPointsIndex >= dice
				val tooManyDemotions = (originalIndex + maxDemotions) >= newListIndex
				if(diceIndexFound || tooManyDemotions) {
					true
				}
				else {
					findIndex += 1
					false
				}
			})
			indexProbAndScores.remove(findIndex)
			
			randomlySorted += removed.get._2
		}
		
		TopScores[INPUT, O](originId, randomlySorted)
	}
	
}
