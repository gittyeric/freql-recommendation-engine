package com.lmi.engine.worker.output.trigger.protocols

import com.lmi.engine.freql.FreqlSelect
import com.lmi.engine.graph.{Edge, Node}
import com.lmi.engine.worker.output.trigger.TriggeredQuery

class HttpQuery[INPUT <: Node, TObj <: Node]
(query: FreqlSelect[INPUT, _ <: Edge, TObj, _ <: Node], url: String)
	extends TriggeredQuery[INPUT, TObj, HttpProtocol](query, new HttpProtocol(url))

sealed case class HttpProtocol(url: String) extends TriggerProtocol {}