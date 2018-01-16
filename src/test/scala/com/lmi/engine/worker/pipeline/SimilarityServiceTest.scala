package com.lmi.engine.worker.pipeline

import com.lmi.engine.worker.similar.SimilarityService
import com.lmi.util.IgnitableTest
import helpers.FakeGraph._
import org.junit.Test

class SimilarityServiceTest extends IgnitableTest {
	
	val service = new SimilarityService(FooToBar)
	
	@Test
	def singlePairScores(): Unit = {
		assert(service.getCachedRecommendations(idFoo).scores.isEmpty,
			"Scores exist with no coocurrences?")
		
		(0 to 50).foreach(i => {
			service.updateScoresAsync(idFoo, idFoo2, i, i, i, i,
				returnLatch = true).get.await()
			
			val fooScores = service.getCachedRecommendations(idFoo).scores
			val foo2Scores = service.getCachedRecommendations(idFoo2).scores
			assert(fooScores.size == 1,
				"Scores don't exist with coocurrences?")
			assert(fooScores(0).points ==
				foo2Scores(0).points,
				"Foo1's score != Foo2's score")
			assert(fooScores(0).points == 0,
				"Nonzero Score?")
		})
	}
	
	@Test
	def multiplePairsScores(): Unit = {
		//TODO
	}
	
}
