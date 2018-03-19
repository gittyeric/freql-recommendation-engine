package com.lmi.examples.whitepaper

import com.lmi.engine._
import com.lmi.engine.freql.map.filter.{InputEqualsOutput, InputRelatesTo, Not}
import com.lmi.engine.freql.{From, Suggest}
import com.lmi.engine.graph.{Edge, Node}
import com.lmi.engine.worker.RelationService
import com.lmi.engine.worker.event.EventRouter
import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.input.kafka.KafkaEventStream
import com.lmi.engine.worker.output.trigger.protocols.HttpQuery
import com.lmi.engine.worker.parse.{DelimiterParser, EventRoute}

object WhitepaperApp {
	
	def createApp() = {
		val tsvParser = new DelimiterParser('\t')
		val inputStreams = Seq[EventStream](new KafkaEventStream("event", tsvParser.parseEvent, "192.241.230.141:2181"))
		new WhitepaperApp(inputStreams)
	}
	
}

//An example of using Figshare API data to recommend whitepapers, authors, etc.
class WhitepaperApp(inputs: Seq[EventStream]) extends FreqlApp {
	
	import WhitepaperGraph._
	
	//Primary sources of similarity
	val authored = new RelationService(P_AuthoredBy_A, maxCachedRecs = 0)
	val attached = new RelationService(P_Has_F, maxCachedRecs = 0)
	val referenced = new RelationService(P_Refs_R, maxCachedRecs = 0)
	val tagged = new RelationService(P_Has_T, maxCachedRecs = 0)
	val categorized = new RelationService(P_Has_C, maxCachedRecs = 0)
	val grouped = new RelationService(P_In_G, maxCachedRecs = 0)
	
	//Some basic filters
	val NotInputPaper = Not(InputEqualsOutput())
	
	//Recommend papers based on authors
	val papersByAuthors =
		Suggest(Paper,
			From(authored)
				Where NotInputPaper)
	
	//Recommend papers based on what they reference
	val papersByReference =
		Suggest(Paper,
			From(referenced)
				Where NotInputPaper)
	
	//Recommend papers based on tags
	val papersByTag =
		Suggest(Paper,
			From(tagged)
				Where NotInputPaper)
	
	//Recommend papers based on categories
	val papersByCat =
		Suggest(Paper,
			From(categorized)
				Where NotInputPaper)
	
	//Recommend papers based on their publisher's group
	val papersByGroup =
		Suggest(Paper,
			From(grouped)
				Where NotInputPaper)
	
	//Recommend papers based on any and all available info
	val papersByAll =
		Suggest(Paper,
			From(papersByAuthors) Join papersByReference Join papersByCat Join papersByTag Join papersByGroup)
	
	//Recommend file attachments based on a paper
	val InputPaperDoesNotHaveFile = Not(InputRelatesTo(attached))
	val filesByAll =
		Suggest(File,
			From(papersByAll) To attached
				Where InputPaperDoesNotHaveFile)
	
	//Recommend categories based on a paper
	val InputPaperDoesNotHaveCat = Not(InputRelatesTo(categorized))
	val catsByAll =
		Suggest(Category,
			From(papersByAll) To categorized
				Where InputPaperDoesNotHaveCat)
	
	//Recommend categories based on a paper
	val InputPaperDoesNotHaveTag = Not(InputRelatesTo(tagged))
	val tagsByAll =
		Suggest(Tag,
			From(papersByAll) To tagged
				Where InputPaperDoesNotHaveTag)
	
	//Recommend groups similar to a paper
	val InputPaperDoesNotHaveGroup = Not(InputRelatesTo(grouped))
	val groupsByAll =
		Suggest(Group,
			From(papersByAll) To grouped
				Where InputPaperDoesNotHaveGroup)
	
	//Recommend authors similar to a paper
	val InputPaperDoesNotHaveAuthor = Not(InputRelatesTo(authored))
	val authorsByAll =
		Suggest(Author,
			From(papersByAll) To authored
				Where InputPaperDoesNotHaveAuthor)
	
	override def name(): String = "Figshare Whitepaper recommender"
	
	override def inputEvents: EventInputs = {
		val router = new EventRouter(Seq(
			EventRoute(AuthoredEvent, P_AuthoredBy_A.inverse()),
			EventRoute(ReferencedEvent, P_Refs_R),
			EventRoute(TaggedEvent, P_Has_T),
			EventRoute(AttachedEvent, P_Has_F),
			EventRoute(CategorizedEvent, P_Has_C),
			EventRoute(GroupedEvent, P_In_G.inverse())
		))
		
		EventInputs(inputs, router)
	}
	
	override def outputs: Outputs = {
		val triggeredOutputs = Seq(
			//Offer recommendations through HTTP interface
			new HttpQuery(papersByAuthors, "by_authors"),
			new HttpQuery(papersByCat, "by_cats"),
			new HttpQuery(papersByReference, "by_refs"),
			new HttpQuery(papersByTag, "by_tags"),
			new HttpQuery(papersByAll, "by_all"),
			
			new HttpQuery(filesByAll, "for_files"),
			new HttpQuery(catsByAll, "for_cats"),
			new HttpQuery(tagsByAll, "for_tags"),
			new HttpQuery(groupsByAll, "for_groups"),
			new HttpQuery(authorsByAll, "for_authors")
		)
		
		return Outputs(triggeredOutputs)
	}
	
	override def pipelines(): Seq[RelationService[_ <: Node, _ <: Edge, _ <: Node]] =
		Seq(authored, attached, referenced, tagged, categorized, grouped)
	
}

