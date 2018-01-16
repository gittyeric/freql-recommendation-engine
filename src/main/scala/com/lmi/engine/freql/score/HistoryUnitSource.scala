package com.lmi.engine.freql.score

import com.lmi.engine.freql.util.FreqlUtils
import com.lmi.engine.graph.{Edge, Id, Node}
import com.lmi.engine.worker.history.HistorySource

import scala.collection.mutable.ListBuffer

//Loads all of an origin's items with score 1 as if they were similarity scores
class HistoryUnitSource[INPUT <: Node, T <: Edge, OUTPUT <: Node]
(historySource: HistorySource[INPUT, T, OUTPUT])
	extends TopScoreSource[INPUT, T, OUTPUT, INPUT] {
	
	def getRecommendations(id: Id[INPUT], maxCount: Int): TopScores[INPUT, OUTPUT] = {
		val items = historySource.getDestinationIdsFor(id, maxCount)
		val scores = new ListBuffer[Score[OUTPUT]]()
		
		while (items.hasNext) {
			val next = items.next()
			scores += Score(Id[OUTPUT](next.id), 1.0)
		}
		
		FreqlUtils.normalize(TopScores[INPUT, OUTPUT](id, scores))
	}
	
}
