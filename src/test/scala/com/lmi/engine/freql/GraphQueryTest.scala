package com.lmi.engine.freql

import com.lmi.engine.freql.map.filter.In
import com.lmi.engine.graph.{Id, Node}
import com.lmi.engine.worker.{PipelineContext, RelationService}
import com.lmi.engine.{EngineTuning, TestTuning}
import com.lmi.util.IgnitableTest
import org.apache.ignite.Ignite
import org.junit.Test

import scala.collection.mutable

class GraphQueryTest extends IgnitableTest {
	
	import helpers.FakeGraph._
	
	val superIgnite = ignite
	
	implicit val context = new PipelineContext {
		override def ignite: Ignite = superIgnite
		
		override def tuning: EngineTuning = TestTuning()
		
		override def maxObjCount(objType: Node): Int = Int.MaxValue
	}
	
	@Test
	def retrieveManyDestRelations() = {
		val fooToBars = new RelationService(FooToBar)
		fooToBars.start()
		
		val myBarCount = 50
		val addedSet = new mutable.HashSet[Id[Bar.type]]()
		(1 to myBarCount).foreach(i => {
			val newId = Id[Bar.type](i.toString)
			fooToBars.items.putHistoryAsync(idFoo, newId).get
			addedSet.add(newId)
		})
		
		val myBarsQuery = Select(Bar, FromItems(fooToBars))
		val retrieved = myBarsQuery.find(idFoo, Int.MaxValue)
		
		assert(retrieved.scores.size == myBarCount,
			"Wrong # of results")
		
		retrieved.scores.foreach(score => {
			addedSet.remove(score.similarTargetId)
		})
		
		assert(addedSet.isEmpty,
			"Added Foo-Bar relation not seen in results")
		
		//Try filtered queries
		val myBarsInQuery = myBarsQuery Where (In(Id("1"), Id("2")))
		val filtered = myBarsInQuery.find(idFoo, myBarCount + 1).scores
		assert(filtered.size == 2,
			"Wrong filtered size")
	}
	
}
