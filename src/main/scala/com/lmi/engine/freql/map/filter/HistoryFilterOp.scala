package com.lmi.engine.freql.map.filter

import com.lmi.engine.freql.score.Score
import com.lmi.engine.graph.{Edge, Id, Node}
import com.lmi.engine.worker.history.HistoricProvider

abstract class HistoryFilterOp[INPUT <: Node, OUTPUT <: Node]
(historyService: HistoricProvider[INPUT, _ <: Edge, OUTPUT],
 val trueCacheExpirationMs: Int,
 val falseCacheExpirationMs: Int)
	extends CachedFilterOp {
	
	override def uncachedAllow[INPUT2 <: Node, OUTPUT2 <: Node](inputId: Id[INPUT2], ts: Score[OUTPUT2]): Boolean = {
		shouldAllow(inputId.asInstanceOf[Id[INPUT]], ts.asInstanceOf[Score[OUTPUT]])
	}
	
	def shouldAllow(inputId: Id[INPUT], ts: Score[OUTPUT]): Boolean
	
}

case class InputRelatesTo[INPUT <: Node, OUTPUT <: Node]
(historyService: HistoricProvider[INPUT, _ <: Edge, OUTPUT],
 trueExpirationMs: Int = 30000,
 falseExpirationMs: Int = 1000)
	extends HistoryFilterOp[INPUT, OUTPUT](historyService, trueExpirationMs, falseExpirationMs) {
	
	override def shouldAllow(inputId: Id[INPUT], ts: Score[OUTPUT]): Boolean = {
		historyService.items.exists(inputId, ts.similarTargetId)
	}
	
}

case class InputEqualsOutput()
	extends FilterOp() {
	
	override def keep[INPUT <: Node, O <: Node](inputId: Id[INPUT], ts: Score[O]): Boolean = {
		inputId.id.equals(ts.similarTargetId.id)
	}
	
}

case class In[INPUT <: Node, O <: Node](matches: Id[O]*)
	extends FilterOp() {
	
	override def keep[INPUT <: Node, O <: Node](inputId: Id[INPUT], ts: Score[O]): Boolean = {
		!matches.contains(ts.similarTargetId)
	}
	
}