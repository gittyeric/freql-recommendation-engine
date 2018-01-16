package com.lmi.engine.worker.output.trigger.protocols

import com.lmi.engine.graph.Node
import com.lmi.engine.worker.output.trigger.TriggeredQuery

object TriggeredHttp {
	
	type HttpQuery[INPUT <: Node, TObj <: Node] = TriggeredQuery[INPUT, TObj, HttpProtocol]
	
}

sealed case class HttpProtocol(url: String) extends TriggerProtocol {}