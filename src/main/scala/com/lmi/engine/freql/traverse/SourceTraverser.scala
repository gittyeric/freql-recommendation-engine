package com.lmi.engine.freql.traverse

import com.lmi.engine.freql.score.{Score, TopScoreSource, TopScores}
import com.lmi.engine.freql.util.FreqlUtils
import com.lmi.engine.graph.{Edge, Id, Node}
import com.lmi.engine.worker.history.HistoricProvider

import scala.collection.mutable

class SourceTraverser
[FI <: Node, TObj <: Node, E <: Edge, TRAVERSE_E <: Edge, GIVEN <: Node, INPUT <: Node, TRAVERSED_TO <: Node]
(source: TopScoreSource[INPUT, E, TObj, GIVEN],
 history: HistoricProvider[TObj, TRAVERSE_E, TRAVERSED_TO],
 op: TraverseOp[TRAVERSED_TO],
 maxItemsPerTarget: Int)
	extends TopScoreSource[INPUT, TRAVERSE_E, TRAVERSED_TO, GIVEN] {
	
	override def similarTo(originId: Id[INPUT], maxCount: Int): TopScores[INPUT, TRAVERSED_TO] = {
		val scores = source.similarTo(originId, maxCount)
		
		val topItemScoresByItemId = new mutable.HashMap[Id[TRAVERSED_TO], Score[TRAVERSED_TO]]()
		
		scores.scores.foreach(ts => {
			val targetItems = history.items.getDestinationIdsFor(ts.similarTargetId, maxItemsPerTarget)
			
			targetItems.foreach(destinationId => {
				val existing = topItemScoresByItemId.get(destinationId)
				val updated = op.update(existing, Score[TRAVERSED_TO](destinationId, ts.points))
				
				topItemScoresByItemId.put(destinationId, updated)
			})
		})
		
		val updated = FreqlUtils.trimScores(topItemScoresByItemId, maxCount)
		FreqlUtils.normalize(TopScores(originId, updated))
	}
	
}
