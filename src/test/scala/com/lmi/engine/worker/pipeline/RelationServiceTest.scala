package com.lmi.engine.worker.pipeline

import com.lmi.engine.graph.{Id, Node}
import com.lmi.engine.worker.{PipelineContext, RelationService}
import com.lmi.engine.{EngineTuning, TestTuning}
import com.lmi.util.IgnitableTest
import helpers.FakeGraph._
import org.apache.ignite.Ignite
import org.junit.Test

import scala.collection.mutable

object PipelineConstants {
	
	//Note: Sorry! These are sloowww tests due to eventual consistency
	//If you're sure your hardware is fast, change this constant temporarily
	val PIPELINE_PROCESSING_DELAY = 750
	
}

class RelationServiceTest extends IgnitableTest {
	
	import PipelineConstants._
	
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
		implicit val context = noLRUContext
		val pipeline = new RelationService(FooToBar)
		fillPipe(pipeline)
	}
	
	private def fillPipe
	(pipeline: RelationService[Foo.type, ToEdge.type, Bar.type])(implicit context: PipelineContext): Unit = {
		pipeline.start()
		
		val originCount = 10
		(1 to originCount).foreach(i => {
			pipeline.items.putHistoryAsync(Id(i.toString), idBar).get()
		})
		
		Thread.sleep(PIPELINE_PROCESSING_DELAY)
		
		(1 to originCount).foreach(i => {
			val seen: mutable.Set[String] = mutable.HashSet()
			val recs = pipeline.similarTo(Id(i.toString), originCount - 1).scores
			recs.foreach(rec => {
				seen += rec.similarTargetId.id
			})
			assert(seen.size == (originCount - 1),
				"Wrong # of recommendations? " + seen.size)
		})
	}
	
	@Test
	def fillPipeNoCachedRecs(): Unit = {
		implicit val context = noLRUContext
		val pipeline = new RelationService(FooToBar, maxCachedRecs = 0)
		fillPipe(pipeline)
	}
	
	@Test
	def fillToEvictionCachedRecs(): Unit = {
		implicit val context = dualFooContext
		val pipeline = new RelationService(FooToBar)
		fillToEviction(pipeline)
	}
	
	@Test
	def fillToEvictionNoCachedRecs(): Unit = {
		implicit val context = dualFooContext
		val pipeline = new RelationService(FooToBar, maxCachedRecs = 0)
		fillToEviction(pipeline)
	}
	
	private def fillToEviction
	(pipeline: RelationService[Foo.type, ToEdge.type, Bar.type])
	(implicit context: PipelineContext) = {
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
		
		val idFoo1Recs = pipeline.similarTo(idFoo, 50)
		val idFoo2Recs = pipeline.similarTo(idFoo2, 50)
		
		assert(idFoo1Recs.scores(0).similarTargetId
			.equals(idFoo2), "Wrong recommendation?")
		assert(idFoo2Recs.scores(0).similarTargetId
			.equals(idFoo), "Wrong recommendation?")
		
		assert(idFoo1Recs.scores.size == 1, "More than 1 rec for LRU size 2?")
		assert(idFoo2Recs.scores.size == 1, "More than 1 rec for LRU size 2?")
	}
	
}
