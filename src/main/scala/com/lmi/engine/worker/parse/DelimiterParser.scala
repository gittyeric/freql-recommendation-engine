package com.lmi.engine.worker.parse

import org.apache.log4j.Logger

import scala.util.{Failure, Try}

class DelimiterParser(delimiter: Char) {
	
	private val logger: Logger = Logger.getLogger("TSVParser")
	
	def parseEvent(line: String): Try[ParsedEvent] = {
		val split: Array[String] = line.split(delimiter)
		
		if(split.length < 3) {
			Failure(new IllegalArgumentException("Could not parse both fields from event"))
		}
		else if(split(1).trim.isEmpty) {
			Failure(new IllegalArgumentException("First field was empty"))
		}
		else if(split(2).trim.isEmpty) {
			Failure(new IllegalArgumentException("Second field was empty"))
		}
		else {
			val eventType: String = split(0)
			try {
				Try(new ParsedEvent(eventType, split(1), split(2)))
			}
			catch {
				case e: Throwable =>
					logger.error(s"Could not parse TSV: $line")
					Failure(e)
			}
		}
	}
	
}
