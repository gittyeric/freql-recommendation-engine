package com.lmi.engine.worker.pipeline

import com.lmi.engine.graph.{Edge, Id, Node, Relation}
import com.lmi.engine.worker.history.HistoryService
import com.lmi.util.IgnitableTest
import helpers.FakeGraph._
import org.junit.Test

class HistoryServiceTest extends IgnitableTest {
	
	@Test
	def putAndRemove(): Unit = {
		putRemove(FooToFoo, removeByOrigin = false)
		putRemove(FooToFoo, removeByOrigin = true)
		putRemove(FooToFoo.inverse(), removeByOrigin = false)
		putRemove(FooToFoo.inverse(), removeByOrigin = true)
	}
	
	def putRemove[O <: Node, E <: Edge, D <: Node]
	(relation: Relation[O, E, D],
	 removeByOrigin: Boolean): Unit = {
		val historyService = new HistoryService(relation)
		val originId = Id[O]("1")
		val destId = Id[D]("2")
		
		historyService.putHistoryAsync(originId, destId).get()
		assert(historyService.exists(originId, destId), "Not found")
		if(removeByOrigin)
			historyService.removeByOriginIdAsync(originId,
				returnLatch = true).get.await()
		else
			historyService.removeByDestinationIdAsync(destId,
				returnLatch = true).get.await()
		assert(!historyService.exists(originId, destId), "Removed found")
	}
	
	@Test
	def putEdgeAndGetInverse(): Unit = {
		val historyService = new HistoryService(FooToBar)
		historyService.putHistoryAsync(idFoo, idBar).get()
		
		val inverseService = new HistoryService(FooToBar.inverse())
		assert(inverseService.getDestinationIdsFor(idBar).size == 1, "Inverse relation not found")
		inverseService.removeByOriginIdAsync(idBar,
			returnLatch = true).get.await()
		
		assert(historyService.getOriginIdsFor(idBar).isEmpty, "Relation not deleted?")
	}
	
}
