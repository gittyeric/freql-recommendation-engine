package com.lmi.engine.worker.parse

import com.lmi.engine.graph.{Edge, Node, Relation}
import com.lmi.engine.worker.event.EventType

case class EventRoute(eventType: EventType, relation: Relation[_ <: Node, _ <: Edge, _ <: Node]) {}
