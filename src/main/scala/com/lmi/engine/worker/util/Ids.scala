package com.lmi.engine.worker.util

object Ids {
	
	val SEPARATOR = "_"
	
	def createKeyFrom(ids: String*): String = {
		val builder = new StringBuilder
		
		ids.foreach((id: String) => {
			builder.append(escape(id))
				.append(SEPARATOR)
		})
		
		builder.substring(0, builder.size - 1)
	}
	
	private def escape(id: String): String = {
		id.replaceAll(SEPARATOR, "\0")
	}
	
	def getIdFromKey(key: String, idPosition: Int): String = {
		unescape(key.split(SEPARATOR)(idPosition))
	}
	
	private def unescape(id: String): String = {
		id.replaceAll("\0", SEPARATOR)
	}
	
}
