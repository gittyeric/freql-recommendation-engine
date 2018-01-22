package com.lmi.engine.worker.output.stream

import com.lmi.engine.freql.FreqlSelect
import com.lmi.engine.graph.{Edge, Id, Node}

final case class ReactiveQueryStream[INPUT <: Node, TObj <: Node, STREAM <: OutputStream[INPUT, TObj]]
(query: FreqlSelect[INPUT, _ <: Edge, TObj, _ <: Node],
 outStream: STREAM,
 maxCount: Int) extends Serializable {
	
	def react(updatedId: Id[INPUT]): Unit = {
		val result = query.find(updatedId, maxCount)
		outStream.saveOutputAsync(result)
	}
	
}
