package com.lmi.engine.worker.similar

import java.util.function.Consumer
import javax.cache.Cache.Entry

import com.lmi.engine.freql.score.{Score, TopScores}
import com.lmi.engine.freql.util.FreqlUtils
import com.lmi.engine.graph._
import com.lmi.engine.worker.util.Ids
import com.lmi.engine.worker.util.ignite.{BatchLatch, BatchLatchFactory, CappedIterator}
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.query.{SqlFieldsQuery, SqlQuery}
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.{Ignite, IgniteCache}
import org.apache.mahout.math.stats.LogLikelihood

import scala.collection.mutable

object SimilarityUtil {
	
	def getOtherOriginIdFor[ORIGIN <: Node]
	(simScore: SimilarityScore): Id[ORIGIN] = {
		Id(Ids.getIdFromKey(simScore.getId, 1))
	}
	
	def getCacheConfigFor[T <: Edge, O1 <: Node, O2 <: Node](relation: Relation[O1, T, O2]): CacheConfiguration[String, SimilarityScore] = {
		val cacheConfig = new CacheConfiguration[String, SimilarityScore]()
		cacheConfig.setName(getCacheNameFor(relation))
		cacheConfig.setCacheMode(CacheMode.PARTITIONED)
		cacheConfig.setBackups(0)
		cacheConfig.setIndexedTypes(classOf[String], classOf[SimilarityScore])
		
		cacheConfig
	}
	
	def getCacheNameFor(relation: Relation[_, _, _]): String = {
		"Similarity_" + relation.id
	}
	
	private[similar] def getSimilarityScoreKey[ORIGIN <: Node]
	(originId: Id[ORIGIN], otherTargetId: Id[ORIGIN]): String = {
		Ids.createKeyFrom(originId.id, otherTargetId.id)
	}
	
}

class SimilarityService[ORIGIN <: Node, E <: Edge, DEST <: Node]
(relation: Relation[ORIGIN, E, DEST])(implicit ignite: Ignite) {
	
	private val cacheName = SimilarityUtil.getCacheConfigFor(relation)
	private val scores: IgniteCache[String, SimilarityScore] =
		ignite.getOrCreateCache(cacheName)
	
	def getCachedRecommendations(
		                            originId: Id[ORIGIN], maxCount: Int = Int.MaxValue): TopScores[ORIGIN, ORIGIN] = {
		
		val topSimScores = getMostSimilar(originId, maxCount)
		val topScores = topSimScores.map(topSimScore =>
			Score[ORIGIN](
				SimilarityUtil.getOtherOriginIdFor(topSimScore),
				topSimScore.getScore
			))
		
		FreqlUtils.normalize(
			TopScores[ORIGIN, ORIGIN](originId, topScores))
	}
	
	private def getMostSimilar(originId: Id[ORIGIN], maxCount: Int): Seq[SimilarityScore] = {
		val query = new SqlQuery[String, SimilarityScore](classOf[SimilarityScore],
			"SELECT * FROM " + classOf[SimilarityScore].getSimpleName +
				" WHERE " + SimilarityScore.ORIGIN_ID_FIELD + " = ?" +
				" ORDER BY " + SimilarityScore.SCORE_FIELD + " DESC LIMIT " + maxCount)
		
		query.setArgs(originId.id)
		query.setPageSize(maxCount)
		
		val toReturn = new mutable.ListBuffer[SimilarityScore]()
		
		scores.query(query).forEach(new Consumer[Entry[String, SimilarityScore]] {
			override def accept(t: Entry[String, SimilarityScore]): Unit = {
				toReturn += t.getValue
			}
		})
		
		toReturn.result()
	}
	
	def updateScoresAsync(objA: Id[ORIGIN], objB: Id[ORIGIN],
	                      aAndBCoocurrences: Long, globalCoocurrences: Long,
	                      objACoocurrences: Long, objBCoocurrences: Long,
	                      returnLatch: Boolean = false): Option[BatchLatch] = {
		val latch = BatchLatchFactory.newOptional(returnLatch)
		val scoreAB = getScoresFor(objA, objB, aAndBCoocurrences, globalCoocurrences, objACoocurrences, objBCoocurrences)
		
		//Similarities are symmetric, so save both
		val updatedAScore = scores.putAsync(scoreAB._1.getId, scoreAB._1)
		val updatedBScore = scores.putAsync(scoreAB._2.getId, scoreAB._2)
		
		latch.foreach(_.listenForFuture(updatedAScore))
		latch.foreach(_.listenForFuture(updatedBScore))
		
		latch
	}
	
	def getScoresFor(objA: Id[ORIGIN], objB: Id[ORIGIN],
	                 aAndBCoocurrences: Long, globalCoocurrences: Long,
	                 objACoocurrences: Long, objBCoocurrences: Long): (SimilarityScore, SimilarityScore) = {
		val score = logLikelihoodRatio(objACoocurrences, objBCoocurrences,
			aAndBCoocurrences, globalCoocurrences)
		
		val aKey = SimilarityUtil.getSimilarityScoreKey(objA, objB)
		val bKey = SimilarityUtil.getSimilarityScoreKey(objB, objA)
		
		val score1 = new SimilarityScore(aKey, objA.id, score)
		val score2 = new SimilarityScore(bKey, objB.id, score)
		
		(score1, score2)
	}
	
	private def logLikelihoodRatio(aCoocurrences: Long, bCoocurrences: Long,
	                               aAndBCoocurrences: Long, globalCoocurrences: Long): Float = {
		
		//This removes certain nasty edge cases of async execution
		val trueGlobalCount = Math.max(globalCoocurrences, aCoocurrences + bCoocurrences - aAndBCoocurrences)
		
		val aCorrelation = aCoocurrences - aAndBCoocurrences
		val bCorrelation = bCoocurrences - aAndBCoocurrences
		val nonABGlobalCorrelation = trueGlobalCount - aCoocurrences - bCoocurrences + aAndBCoocurrences
		
		//The async eventually consistent model can temporarily produce weird negative values,
		//in which case we'll just set to zero, since it'll quickly sort itself out on next update
		if(aCorrelation < 0 || bCorrelation < 0)
			0
		else
			LogLikelihood.logLikelihoodRatio(aAndBCoocurrences, aCorrelation, bCorrelation, nonABGlobalCorrelation).toFloat
	}
	
	def trimBottomScoresAsync(originId: String, maxTopSimilarities: Int,
	                          returnLatch: Boolean = false): Option[BatchLatch] = {
		val query = new SqlFieldsQuery(
			"SELECT _key " +
				"FROM " + classOf[SimilarityScore].getSimpleName +
				" WHERE " + SimilarityScore.ORIGIN_ID_FIELD + " = ?" +
				" ORDER BY " + SimilarityScore.SCORE_FIELD + " DESC" +
				" OFFSET " + maxTopSimilarities)
		
		query.setArgs(originId)
		
		removeByIterAsync(new CappedIterator(
			scores.query(query).iterator(), Int.MaxValue),
			returnLatch)
	}
	
	private def removeByIterAsync(idIter: Iterator[String],
	                              returnLatch: Boolean): Option[BatchLatch] = {
		val latch = BatchLatchFactory.newOptional(returnLatch)
		
		idIter.foreach(id => {
			val removed = scores.removeAsync(id)
			latch.foreach(_.listenForFuture(removed))
		})
		
		latch
	}
	
	def removeScoresAsync(originId: String,
	                      returnLatch: Boolean = false): Option[BatchLatch] = {
		val query = new SqlFieldsQuery(
			"SELECT _key " +
				"FROM " + classOf[SimilarityScore].getSimpleName +
				" WHERE " + SimilarityScore.ORIGIN_ID_FIELD + " = ?")
		
		query.setArgs(originId)
		
		removeByIterAsync(
			new CappedIterator(scores.query(query).iterator(), Int.MaxValue), returnLatch
		)
	}
	
}
