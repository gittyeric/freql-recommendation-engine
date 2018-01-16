package com.lmi.engine.worker.pipeline

import com.lmi.engine.graph.{Edge, Id, Node}
import com.lmi.engine.worker.coocur.CoocurService
import com.lmi.util.IgnitableTest
import helpers.FakeGraph._
import org.junit.Test

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class CoocurServiceTest extends IgnitableTest {
	
	val service = new CoocurService(FooToBar)
	
	@Test
	def manyOriginsToDestination(): Unit = {
		val foosSoFar = mutable.ListBuffer[Id[Foo.type]]()
		var trueGlobalCount = 0
		(0 to 10).foreach(i => {
			val fooI = Id[Foo.type](i.toString)
			foosSoFar += fooI
			service.
				incrementCoocurrencesAsync(foosSoFar.iterator, fooI, idBar,
					returnLatch = true).get.await()
			
			assert(service.getGlobalCount == trueGlobalCount,
				"Wrong # of global coocurrences")
			trueGlobalCount += foosSoFar.length
			
			assertOriginsMatchService(foosSoFar, service)
		})
		
		foosSoFar.foreach(removedFoo => {
			trueGlobalCount -= foosSoFar.length
			assert(service.getGlobalCount == trueGlobalCount,
				"Wrong # of global coocurrences")
			
			foosSoFar -= removedFoo
			service.removeObjAsync(removedFoo,
				returnLatch = true).get.await()
			
			assertOriginsMatchService(foosSoFar, service)
		})
		
		assert(service.getGlobalCount == 0,
			"Non-zero global coocurrences when empty?")
	}
	
	//Assumes only 1 destination Obj
	private def assertOriginsMatchService[E <: Edge, O <: Node, D <: Node]
	(foos: Seq[Id[O]], service: CoocurService[E, O, D]): Unit = {
		
		foos.foreach(foo => {
			val iter = service.getCoocurrencesWith(foo)
			var iterCount = 0
			while (iter.hasNext) {
				iter.next()
				iterCount += 1
			}
			assert(iterCount == foos.length - 1,
				"Wrong # of coocur pairs from query")
			assert(service.getCountAsync(foo).get == foos.length - 1,
				"Wrong # of Obj coocurrences")
			assert(service.getPairCount(foo, foo) == 0,
				"Coocurred with self?")
			foos.filter(!_.equals(foo)).foreach(innerFoo => {
				assert(service.getPairCount(foo, innerFoo) == 1,
					"Wrong A-to-B coocurrence count")
				assert(Await.result(service.getObjCounts(foo, innerFoo).future, Duration.Inf).equals(
					service.getCountAsync(foo).get, service.getCountAsync(innerFoo).get),
					"getObjCounts(a, b) != (getObjCount(a), getObjCount(b))")
			})
		})
	}
	
	@Test
	def originsToMultipleDestinations(): Unit = {
		val foos = List(idFoo, idFoo2)
		val foo1List = List(idFoo)
		var trueGlobalCount = 0
		(0 to 10).foreach(i => {
			val barI = Id[Bar.type](i.toString)
			service.incrementCoocurrencesAsync(List().iterator, idFoo, barI,
				returnLatch = true).get.await()
			service.incrementCoocurrencesAsync(foo1List.iterator, idFoo2, barI,
				returnLatch = true).get.await()
			
			trueGlobalCount += 1
			assert(service.getGlobalCount == trueGlobalCount,
				"Wrong # of global coocurrences")
			
			assert(service.getCountAsync(idFoo).get == service.getCountAsync(idFoo2).get,
				"Foo1's count != Foo2's count")
			assert(service.getCountAsync(idFoo).get == trueGlobalCount,
				"Foo1's count != global count?")
			assert(service.getPairCount(idFoo, idFoo2) == trueGlobalCount,
				"Foo1 and Foo2's coocurrence count != global count?")
		})
		
		service.removeObjAsync(idFoo, returnLatch = true).get.await()
		assert(service.getGlobalCount == 0,
			"Global coocurrences with only 1 Foo?")
		assert(service.getPairCount(idFoo, idFoo2) == 0,
			"Foo1 and Foo2 coocurr with only 1 Foo?")
		assert(service.getCountAsync(idFoo).get == 0,
			"Foo has Obj count after removal")
		assert(service.getCountAsync(idFoo2).get == 0,
			"Foo2 has Obj count with no other Foos?")
	}
	
}
