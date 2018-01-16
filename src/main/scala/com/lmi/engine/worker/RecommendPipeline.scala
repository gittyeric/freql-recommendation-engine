package com.lmi.engine.worker

import com.lmi.engine.freql.score.{Score, TopScoreSource, TopScores}
import com.lmi.engine.graph.{Id, _}
import com.lmi.engine.worker.coocur.CoocurService
import com.lmi.engine.worker.history.{HistoryService, HistorySource}
import com.lmi.engine.worker.lru.LRUService
import com.lmi.engine.worker.output.stream.ReactiveQueryStream
import com.lmi.engine.worker.similar.SimilarityService
import com.lmi.engine.worker.util.ignite.BatchLatch
import org.apache.ignite.lang.{IgniteClosure, IgniteFuture}

import scala.collection.mutable

class RecommendPipeline
[ORIGIN <: Node, E <: Edge, DEST <: Node]
(relation: Relation[ORIGIN, E, DEST],
 reactiveOutputs: Seq[ReactiveQueryStream[ORIGIN, _, _]] = Seq(),

 //Set this above your max query size for faster queries,
 //or set to zero to save memory in exchange for slower queries
 val maxCachedRecs: Int = Int.MaxValue,
 minCoocurrences: Int = 1
)
(implicit context: PipelineContext)
	extends TopScoreSource[ORIGIN, E, ORIGIN, DEST] {
	
	//Self managed Sub-services, exposed for FREQL convenience
	val items: HistorySource[ORIGIN, E, DEST] = history
	val originType = relation.origin
	val destType = relation.destination
	implicit private val ignite = context.ignite
	private val history = new HistoryService(relation)
	private val coocurrences = new CoocurService(relation)
	private val similarities = new SimilarityService(relation)
	private val originLRU =
		if(context.maxObjCount(originType) < Int.MaxValue)
			Some(new LRUService(originType, context.maxObjCount(originType)))
		else None
	
	private val destinationLRU =
		if(context.maxObjCount(destType) < Int.MaxValue)
			Some(new LRUService(destType, context.maxObjCount(destType)))
		else None
	
	var queues: Option[PipelineQueues[ORIGIN, E, DEST]] = None
	
	def start(): Unit = {
		if(queues.isEmpty) {
			queues = Some(new PipelineQueues(
				originLRU, destinationLRU,
				history, coocurrences, similarities,
				maxCachedRecs, reactiveOutputs))
			queues.get.start()
		}
	}
	
	override def getRecommendations(originId: Id[ORIGIN], maxCount: Int): TopScores[ORIGIN, ORIGIN] = {
		if(maxCount <= maxCachedRecs)
			similarities.getCachedRecommendations(originId, maxCount)
		else
			getRecommendationsFromCoocurrences(originId, maxCount)
	}
	
	private def getRecommendationsFromCoocurrences(originId: Id[ORIGIN], maxCount: Int): TopScores[ORIGIN, ORIGIN] = {
		val originCoocurCountF = coocurrences.getCountAsync(originId)
		val originCoocurs = coocurrences.getCoocurrencesWith(originId)
		val globalCoocurCount = coocurrences.getGlobalCount
		
		val batchLatch = new BatchLatch()
		var scoreList = mutable.ListBuffer[Score[ORIGIN]]()
		
		while (originCoocurs.hasNext) {
			val originCoocur = originCoocurs.next().getValue
			val abCount = originCoocur.getCoocurCount
			val bId = Id[ORIGIN](
				if(originCoocur.getNodeAId.equals(originId.id)) originCoocur.getNodeBId
				else originCoocur.getNodeAId)
			val bScoreComplete = coocurrences.getCountAsync(bId)
				.chain(new IgniteClosure[IgniteFuture[Int], Unit] {
					override def apply(e: IgniteFuture[Int]): Unit = {
						val bCount = e.get()
						scoreList += Score(bId,
							similarities.getScoresFor(originId, bId, abCount, globalCoocurCount, originCoocurCountF.get(), bCount)._1.getScore)
					}
				})
			batchLatch.listenForFuture(bScoreComplete)
		}
		
		batchLatch.await()
		
		new TopScores(originId, scoreList.sortWith((score1, score2) =>
			score1.points > score2.points))
	}
	
}
