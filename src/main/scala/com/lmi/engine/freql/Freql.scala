package com.lmi.engine.freql

import com.lmi.engine.freql.join.{JoinByKeyOp, JoinByMaxOp, JoinedScoreSource}
import com.lmi.engine.freql.map.filter.FilterOp
import com.lmi.engine.freql.map.{DitherOp, SourceMapper}
import com.lmi.engine.freql.pivot.{SourcesPivoter, WeightedPipeOp}
import com.lmi.engine.freql.score.{HistoryUnitSource, TopScoreSource, TopScores}
import com.lmi.engine.freql.traverse.{DestinationSumOp, SourceTraverser, TraverseOp}
import com.lmi.engine.graph._
import com.lmi.engine.worker.history.HistorySource

class FreqlFrom[INPUT <: Node, E <: Edge, TObj <: Node, GIVEN_ITEM <: Node]
(val source: TopScoreSource[INPUT, E, TObj, GIVEN_ITEM]) {
	
	def `+`[R2 <: Edge, GIVEN2 <: Node]
	(rFrom: FreqlFrom[INPUT, R2, TObj, GIVEN2]) = Join(rFrom)
	
	def Join[R2 <: Edge, GIVEN_ITEM2 <: Node](
		                                         rightSource: FreqlFrom[INPUT, R2, TObj, GIVEN_ITEM2],
		                                         by: JoinByKeyOp = JoinByMaxOp()):
	FreqlFrom[INPUT, MergedEdge[E, R2], TObj, MergedNode[GIVEN_ITEM, GIVEN_ITEM2]] = {
		new FreqlFrom(new JoinedScoreSource(source, rightSource.source, by))
	}
	
	def `-->`[NEW_E <: Edge, NEWOUT <: Node]
	(historySource: HistorySource[TObj, NEW_E, NEWOUT]) = To(historySource)
	
	//follows a relationship from origins to their destinations,
	//with destinations inheriting the (weighted sum of) scores from their origins' scores
	def To[NEW_E <: Edge, NEWOUT <: Node]
	(historySource: HistorySource[TObj, NEW_E, NEWOUT],
	 destinationScorer: TraverseOp[NEWOUT] = DestinationSumOp[NEWOUT](),
	 RelationLimit: Int = Int.MaxValue)
	: FreqlFrom[INPUT, NEW_E, NEWOUT, GIVEN_ITEM] = {
		val followed = new SourceTraverser(source, historySource, destinationScorer, RelationLimit)
		new FreqlFrom(followed)
	}
	
	def `-O->`[NEWOUT <: Node, GIVEN2 <: Node, E2 <: Edge]
	(pivotTo: FreqlFrom[TObj, E2, NEWOUT, GIVEN2]) = Pivot(pivotTo)
	
	//Take some TopScores for origins and find other similar origins given a GIVEN2 destination.
	def Pivot[NEWOUT <: Node, GIVEN2 <: Node, E2 <: Edge]
	(pivotTo: FreqlFrom[TObj, E2, NEWOUT, GIVEN2],
	 maxSimsPerTarget: Int = Int.MaxValue)
	: FreqlFrom[INPUT, MergedEdge[E, E2], NEWOUT, MergedNode[GIVEN_ITEM, GIVEN2]] = {
		new FreqlFrom(new SourcesPivoter(source, pivotTo.source, new WeightedPipeOp[NEWOUT], maxSimsPerTarget))
	}
	
	def Randomize(
		             fairness: Double = 1.0,
		             maxDemotions: Int = Int.MaxValue,
		             seed: Long = 87): FreqlFrom[INPUT, E, TObj, GIVEN_ITEM] = {
		
		new FreqlFrom(new SourceMapper(source, new DitherOp(fairness, maxDemotions, seed)))
	}
	
	def Where(filter: FilterOp): FreqlFrom[INPUT, E, TObj, GIVEN_ITEM] = {
		new FreqlFrom(new SourceMapper(source, filter))
	}
	
}

object FreqlSelect {
	
	import scala.language.implicitConversions
	
	implicit def selectToFrom
	[INPUT <: Node, E <: Edge, OUTPUT <: Node, GIVEN <: Node]
	(freqlSelect: FreqlSelect[INPUT, E, OUTPUT, GIVEN]): FreqlFrom[INPUT, E, OUTPUT, GIVEN] = {
		freqlSelect.from
	}
	
}

trait FreqlSelect[INPUT <: Node, E <: Edge, OUTPUT <: Node, GIVEN <: Node] {
	
	def outputObj: OUTPUT
	
	def from: FreqlFrom[INPUT, E, OUTPUT, GIVEN]
	
	def find(similarTo: Id[INPUT], maxCount: Int): TopScores[INPUT, OUTPUT] = {
		from.source.getRecommendations(similarTo, maxCount)
	}
	
}

case class Select[INPUT <: Node, E <: Edge, OUTPUT <: Node, GIVEN <: Node]
(outputObj: OUTPUT, from: FreqlFrom[INPUT, E, OUTPUT, GIVEN])
	extends FreqlSelect[INPUT, E, OUTPUT, GIVEN] {}

object From {
	
	//Ugliness that allows the From case class to have 2 different constructors
	def apply[INPUT <: Node, E <: Edge, TObj <: Node, GIVEN <: Node]
	(selectSrc: FreqlSelect[INPUT, E, TObj, GIVEN]) =
		new From(selectSrc.from.source)
}

case class From[INPUT <: Node, E <: Edge, TObj <: Node, GIVEN <: Node]
(inputSource: TopScoreSource[INPUT, E, TObj, GIVEN])
	extends FreqlFrom[INPUT, E, TObj, GIVEN](inputSource) {}

case class FromItems[INPUT <: Node, E <: Edge, OUTPUT <: Node]
(historySource: HistorySource[INPUT, E, OUTPUT])
	extends FreqlFrom[INPUT, E, OUTPUT, INPUT](
		new HistoryUnitSource[INPUT, E, OUTPUT](historySource)) {}

private object FreqlFrom {
	
	implicit def sourceToFrom
	[INPUT <: Node, E <: Edge, OUTPUT <: Node, GIVEN <: Node]
	(source: TopScoreSource[INPUT, E, OUTPUT, GIVEN]): FreqlFrom[INPUT, E, OUTPUT, GIVEN] = {
		From(source)
	}
	
}