package com.lmi.engine.worker.similar

import com.lmi.engine.worker.output.stream.ReactiveQueryStream

import scala.collection.mutable

class ReactiveQueryRouter(streams: Seq[ReactiveQueryStream[_, _, _]]) {
	
	val typeToStreams = new mutable.HashMap[String, mutable.ListBuffer[ReactiveQueryStream[_, _, _]]]
	
	streams.foreach((stream) => {
		//add(stream)
	})
	
	/*private def add(stream: ReactiveQueryStream[_, _, _]) = {
		if(!typeToStreams.contains(stream.)) {
			typeToStreams.put(stream.eventType.toString, new mutable.ListBuffer[ReactiveQueryStream[_, _, _]]())
		}
		
		typeToStreams.get(stream.eventType.toString).get += stream
	}
	
	def process(events: Seq[TriggeredRequest]): Unit = {
		events.foreach((event: TriggeredRequest) => process(event))
	}
	
	def process(event: TriggeredRequest): Unit = {
		val streams = typeToStreams.get(event)
		
		if(streams.isDefined) {
			streams.get.foreach((stream) =>
				stream.respond(event)
			)
		}
	}*/
	
}
