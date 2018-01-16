package com.lmi.engine.worker.pipeline

import com.lmi.engine.graph.{Id, Node}
import com.lmi.engine.worker.{PipelineContext, RecommendPipeline}
import com.lmi.engine.{EngineTuning, TestTuning}
import com.lmi.util.IgnitableTest
import helpers.FakeGraph._
import org.apache.ignite.Ignite
import org.junit.Test

import scala.collection.mutable

class PipelineTest extends IgnitableTest {
	
	//Note: Sorry! These are sloowww tests due to eventual consistency
	//If you're sure your hardware is fast, change this constant temporarily
	val PIPELINE_PROCESSING_DELAY = 750
	
	val superIgnite = ignite
	
	val noLRUContext = new PipelineContext {
		override def ignite: Ignite = superIgnite
		
		override def tuning: EngineTuning = TestTuning()
		
		override def maxObjCount(objType: Node): Int = Int.MaxValue
	}
	
	val dualFooContext = new PipelineContext {
		override def ignite: Ignite = superIgnite
		
		override def tuning: EngineTuning = TestTuning()
		
		override def maxObjCount(objType: Node): Int =
			objType match {
				case _: Foo.type => 2
				case _ => Int.MaxValue
			}
	}
	
	@Test
	def fillPipeCachedRecs(): Unit = {
		val pipeline = new RecommendPipeline(FooToBar)(noLRUContext)
		fillPipe(pipeline)
	}
	
	private def fillPipe(pipeline: RecommendPipeline[Foo.type, ToEdge.type, Bar.type]): Unit = {
		pipeline.start()
		
		val originCount = 10
		(1 to originCount).foreach(i => {
			pipeline.items.putHistoryAsync(Id(i.toString), idBar).get()
		})
		
		Thread.sleep(PIPELINE_PROCESSING_DELAY)
		
		(1 to originCount).foreach(i => {
			val seen: mutable.Set[String] = mutable.HashSet()
			val recs = pipeline.getRecommendations(Id(i.toString), originCount - 1).scores
			recs.foreach(rec => {
				seen += rec.similarTargetId.id
			})
			assert(seen.size == (originCount - 1),
				"Wrong # of recommendations? " + seen.size)
		})
	}
	
	@Test
	def fillPipeNoCachedRecs(): Unit = {
		val pipeline = new RecommendPipeline(FooToBar, maxCachedRecs = 0)(noLRUContext)
		fillPipe(pipeline)
	}
	
	@Test
	def fillToEvictionCachedRecs(): Unit = {
		val pipeline = new RecommendPipeline(FooToBar)(dualFooContext)
		fillToEviction(pipeline)
	}
	
	@Test
	def fillToEvictionNoCachedRecs(): Unit = {
		val pipeline = new RecommendPipeline(FooToBar, maxCachedRecs = 0)(dualFooContext)
		fillToEviction(pipeline)
	}
	
	private def fillToEviction(pipeline: RecommendPipeline[Foo.type, ToEdge.type, Bar.type]) = {
		pipeline.start()
		
		(3 to 10).foreach(i => {
			pipeline.items.putHistoryAsync(Id(i.toString), idBar).get()
		})
		
		//Clear out last loop completely by eviction
		Thread.sleep(100)
		pipeline.items.putHistoryAsync(Id("hello"), idBar2)
		pipeline.items.putHistoryAsync(Id("world"), idBar2)
		Thread.sleep(PIPELINE_PROCESSING_DELAY)
		
		//Previous loop entries should be gone now (except last 2 idBar2's)
		pipeline.items.putHistoryAsync(idFoo, idBar).get()
		pipeline.items.putHistoryAsync(idFoo2, idBar).get()
		Thread.sleep(PIPELINE_PROCESSING_DELAY)
		
		val idFoo1Recs = pipeline.getRecommendations(idFoo, 50)
		val idFoo2Recs = pipeline.getRecommendations(idFoo2, 50)
		
		assert(idFoo1Recs.scores(0).similarTargetId
			.equals(idFoo2), "Wrong recommendation?")
		assert(idFoo2Recs.scores(0).similarTargetId
			.equals(idFoo), "Wrong recommendation?")
		
		assert(idFoo1Recs.scores.size == 1, "More than 1 rec for LRU size 2?")
		assert(idFoo2Recs.scores.size == 1, "More than 1 rec for LRU size 2?")
	}
	
}
