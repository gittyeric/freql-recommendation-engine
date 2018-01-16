package com.lmi.engine.worker.output.stream.http

import java.net.URI

import com.lmi.engine.TriggeredJsonSupport
import com.lmi.engine.freql.FreqlSelect
import com.lmi.engine.freql.score.TopScores
import com.lmi.engine.graph.{Edge, Node}
import com.lmi.engine.worker.output.stream.OutputStream

class HttpOutputStream[INPUT <: Node, TObj <: Node]
(
	url: String,
	querySource: FreqlSelect[INPUT, _ <: Edge, TObj, _ <: Node]
) extends OutputStream[INPUT, TObj] with TriggeredJsonSupport {
	
	val uri = new URI(url)
	
	def saveOutputAsync(scores: TopScores[INPUT, TObj]): Unit = {
		//TODO
	}
	
}
