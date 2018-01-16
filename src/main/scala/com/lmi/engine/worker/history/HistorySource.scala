package com.lmi.engine.worker.history

import com.lmi.engine.graph.{Edge, Id, Node, Relation}
import com.lmi.engine.worker.util.ignite.BatchLatch
import org.apache.ignite.lang.IgniteFuture

trait HistorySource[TARGET <: Node, T <: Edge, ITEM <: Node] {
	
	def exists(tId: Id[TARGET], iId: Id[ITEM]): Boolean
	
	def getOriginIdsFor(destinationId: Id[ITEM], maxCount: Int = Int.MaxValue): Iterator[Id[TARGET]]
	
	def getDestinationIdsFor(originId: Id[TARGET], maxCount: Int = Int.MaxValue): Iterator[Id[ITEM]]
	
	def removeByOriginIdAsync(originId: Id[TARGET], returnLatch: Boolean = false): Option[BatchLatch]
	
	def removeByDestinationIdAsync(destinationId: Id[ITEM], returnLatch: Boolean = false): Option[BatchLatch]
	
	def putHistoryAsync(originId: Id[TARGET], destinationId: Id[ITEM]): IgniteFuture[_]
	
	def getOriginIdFrom(entry: HistoryEntry): Id[TARGET]
	
	def getDestinationIdFrom(entry: HistoryEntry): Id[ITEM]
	
	//def asInverse(): HistorySource[INVERSE_T, T, ITEM, TARGET]
	
	def relation(): Relation[TARGET, T, ITEM]
	
	def historyCacheName(): String
	
}
