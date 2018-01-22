package com.lmi.engine.freql

import com.lmi.engine.graph.Node
import com.lmi.engine.worker.PipelineContext
import com.lmi.engine.{EngineTuning, TestTuning}
import com.lmi.util.IgnitableTest
import org.apache.ignite.Ignite

class RecQueryTest extends IgnitableTest {
	
	val superIgnite = ignite
	
	implicit val context = new PipelineContext {
		override def ignite: Ignite = superIgnite
		
		override def tuning: EngineTuning = TestTuning()
		
		override def maxObjCount(objType: Node): Int = Int.MaxValue
	}
	
	/*@Test
	def join() = {
		val fooToBars = new RecommendPipeline(FooToBar)
		val fooToPhos = new RecommendPipeline(FooToPho)
		fooToBars.start()
		fooToPhos.start()
		
		val joined = Select(Foo, From(fooToBars) Join fooToPhos )
		val reverseJoined = Select(Foo, From(fooToPhos) Join fooToBars)
		
		fooToBars.items.putHistoryAsync(idFoo, idBar).get
		fooToBars.items.putHistoryAsync(idFoo2, idBar).get
		Thread.sleep(PIPELINE_PROCESSING_DELAY)
	
		assert(joined.find(idFoo, 999).scores.size == 1,
			"Joining left failed with empty right")
		assert(reverseJoined.find(idFoo, 999).scores.size == 1,
			"Joining right failed with empty left")
		
		fooToPhos.items.putHistoryAsync(idFoo, idPho1 ).get
		fooToPhos.items.putHistoryAsync(idFoo2, idPho1 ).get
		fooToPhos.items.putHistoryAsync(Id("3"), idPho1 ).get
		Thread.sleep(PIPELINE_PROCESSING_DELAY)
		
		val dualResult = joined.find(idFoo, 999).scores
		assert(dualResult.size == 2,
			"Joining failed")
		assert(dualResult(0).similarTargetId.equals(idFoo2),
			"Foo2 should be first since it shares a Bar and Pho")
		
		Thread.sleep(4000)
	}*/
	
	
}
