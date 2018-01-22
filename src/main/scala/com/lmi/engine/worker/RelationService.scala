package com.lmi.engine.worker

import com.lmi.engine.freql.score.{Score, TopScoreSource, TopScores}
import com.lmi.engine.graph.{Id, _}
import com.lmi.engine.worker.coocur.CoocurService
import com.lmi.engine.worker.history.{HistoricProvider, HistoryService, HistorySource}
import com.lmi.engine.worker.lru.LRUService
import com.lmi.engine.worker.output.stream.ReactiveQueryStream
import com.lmi.engine.worker.similar.SimilarityService
import com.lmi.engine.worker.util.ignite.BatchLatch
import org.apache.ignite.lang.{IgniteClosure, IgniteFuture}

import scala.collection.mutable

class RelationService
[ORIGIN <: Node, E <: Edge, DEST <: Node]
(relation: Relation[ORIGIN, E, DEST],
 reactiveOutputs: Seq[ReactiveQueryStream[ORIGIN, _, _]] = Seq(),

 //Set this above your max query size for faster queries,
 //or set to zero to save memory in exchange for slower queries
 val maxCachedRecs: Int = Int.MaxValue,
 minCoocurrences: Int = 1
)
	extends TopScoreSource[ORIGIN, E, ORIGIN, DEST] with HistoricProvider[ORIGIN, E, DEST] {
	
	val originType = relation.origin
	val destType = relation.destination
	
	var queues: Option[PipelineQueues[ORIGIN, E, DEST]] = None
	private var history: Option[HistoryService[ORIGIN, E, DEST]] = None
	private var coocurrences: Option[CoocurService[E, ORIGIN, DEST]] = None
	private var similarities: Option[SimilarityService[ORIGIN, E, DEST]] = None
	
	def start()(implicit context: PipelineContext): Unit = {
		require(history.isEmpty, "Pipeline already started!")
		
		implicit val ignite = context.ignite
		history = Some(new HistoryService(relation))
		coocurrences = Some(new CoocurService(relation))
		similarities = Some(new SimilarityService(relation))
		
		val originLRU =
			if(context.maxObjCount(originType) < Int.MaxValue)
				Some(new LRUService(originType, context.maxObjCount(originType)))
			else None
		
		val destinationLRU =
			if(context.maxObjCount(destType) < Int.MaxValue)
				Some(new LRUService(destType, context.maxObjCount(destType)))
			else None
		
		queues = Some(new PipelineQueues(
			originLRU, destinationLRU,
			history.get, coocurrences.get, similarities.get,
			maxCachedRecs, reactiveOutputs))
		queues.get.start()
	}
	
	def items: HistorySource[ORIGIN, E, DEST] = {
		require(history.nonEmpty, "Cannot get items until app is started")
		history.get
	}
	
	override def similarTo(originId: Id[ORIGIN], maxCount: Int): TopScores[ORIGIN, ORIGIN] = {
		if(maxCount <= maxCachedRecs)
			similarities.get.getCachedRecommendations(originId, maxCount)
		else
			getRecommendationsFromCoocurrences(originId, maxCount)
	}
	
	private def getRecommendationsFromCoocurrences(originId: Id[ORIGIN], maxCount: Int): TopScores[ORIGIN, ORIGIN] = {
		val originCoocurCountF = coocurrences.get.getCountAsync(originId)
		val originCoocurs = coocurrences.get.getCoocurrencesWith(originId)
		val globalCoocurCount = coocurrences.get.getGlobalCount
		
		val batchLatch = new BatchLatch()
		var scoreList = mutable.ListBuffer[Score[ORIGIN]]()
		
		while (originCoocurs.hasNext) {
			val originCoocur = originCoocurs.next().getValue
			val abCount = originCoocur.getCoocurCount
			val bId = Id[ORIGIN](
				if(originCoocur.getNodeAId.equals(originId.id)) originCoocur.getNodeBId
				else originCoocur.getNodeAId)
			val bScoreComplete = coocurrences.get.getCountAsync(bId)
				.chain(new IgniteClosure[IgniteFuture[Int], Unit] {
					override def apply(e: IgniteFuture[Int]): Unit = {
						val bCount = e.get()
						scoreList += Score(bId,
							similarities.get.getScoresFor(originId, bId, abCount, globalCoocurCount, originCoocurCountF.get(), bCount)._1.getScore)
					}
				})
			batchLatch.listenForFuture(bScoreComplete)
		}
		
		batchLatch.await()
		
		new TopScores(originId, scoreList.sortWith((score1, score2) =>
			score1.points > score2.points))
	}
	
}
