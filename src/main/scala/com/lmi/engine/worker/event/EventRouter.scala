package com.lmi.engine.worker.event

import com.lmi.engine.graph.{Edge, Node, Relation}
import com.lmi.engine.worker.history.HistoryEntry
import com.lmi.engine.worker.parse.{EventRoute, ParsedEvent}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class EventRouter(routes: Seq[EventRoute]) {
	
	private val eventToRoutes = new scala.collection.mutable.HashMap[String, ListBuffer[EventRoute]]
	
	addRoutes(routes)
	
	def route(events: Seq[ParsedEvent]): Seq[(Relation[_ <: Node, _ <: Edge, _ <: Node], HistoryEntry)] = {
		val toReturn = new ListBuffer[(Relation[_ <: Node, _ <: Edge, _ <: Node], HistoryEntry)]
		events.foreach((event: ParsedEvent) => {
			toReturn ++= route(event)
		})
		
		toReturn
	}
	
	def route(event: ParsedEvent): Seq[(Relation[_ <: Node, _ <: Edge, _ <: Node], HistoryEntry)] = {
		val routes = eventToRoutes.get(event.eventType)
		val toReturn = new ListBuffer[(Relation[_ <: Node, _ <: Edge, _ <: Node], HistoryEntry)]()
		
		if(routes.isDefined) {
			routes.get.foreach(route => {
				val toAdd = (route.relation, new HistoryEntry(event.originId(), event.destinationId()))
				toReturn += toAdd
			})
		}
		
		toReturn
	}
	
	def relations(): Set[Relation[_ <: Node, _ <: Edge, _ <: Node]] = {
		val toReturn = new mutable.HashSet[Relation[_ <: Node, _ <: Edge, _ <: Node]]
		
		eventToRoutes.foreach((entry: (String, ListBuffer[EventRoute])) => {
			toReturn ++= entry._2.map(route => route.relation)
		})
		
		toReturn.toSet
	}
	
	private def addRoutes(routes: Seq[EventRoute]): Unit = {
		routes.foreach((route) => {
			addRoute(route)
		})
	}
	
	private def addRoute(route: EventRoute) = {
		if(!eventToRoutes.contains(route.eventType.str)) {
			eventToRoutes.put(route.eventType.str, new ListBuffer[EventRoute]())
		}
		
		eventToRoutes(route.eventType.str) += route
	}
	
}
