package com.lmi.examples.whitepaper

import com.lmi.engine._
import com.lmi.engine.freql.map.filter.{InputEqualsOutput, InputRelatedBy, Not}
import com.lmi.engine.freql.{From, Select}
import com.lmi.engine.worker.event.EventRouter
import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.output.trigger.protocols.HttpProtocol
import com.lmi.engine.worker.output.trigger.protocols.TriggeredHttp.HttpQuery
import com.lmi.engine.worker.parse.EventRoute
import com.lmi.engine.worker.{PipelineContext, RecommendPipeline}

//An example of using Figshare API data to recommend whitepapers, authors, etc.
class WhitepaperApp(inputs: Seq[EventStream]) extends FreqlApp {
	
	import WhitepaperGraph._
	
	protected var instance: WhitepaperAppInstance = null
	
	override def name(): String = "Figshare Whitepaper recommender"
	
	override def eventSources: EventSources = {
		val router = new EventRouter(Seq(
			EventRoute(AuthoredEvent, P_AuthoredBy_A.inverse()),
			EventRoute(ReferencedEvent, P_Refs_R),
			EventRoute(TaggedEvent, P_Has_T),
			EventRoute(AttachedEvent, P_Has_F),
			EventRoute(CategorizedEvent, P_Has_C),
			EventRoute(GroupedEvent, P_In_G.inverse())
		))
		
		EventSources(inputs, router)
	}
	
	override def buildComputationGraph()(implicit context: PipelineContext): ComputationGraph = {
		instance = new WhitepaperAppInstance()
		instance.buildComputationGraph()
	}
	
	class WhitepaperAppInstance(implicit context: PipelineContext) {
		
		//Primary sources of similarity
		val authored = new RecommendPipeline(P_AuthoredBy_A, maxCachedRecs = 0)
		val attached = new RecommendPipeline(P_Has_F, maxCachedRecs = 0)
		val referenced = new RecommendPipeline(P_Refs_R, maxCachedRecs = 0)
		val tagged = new RecommendPipeline(P_Has_T, maxCachedRecs = 0)
		val categorized = new RecommendPipeline(P_Has_C, maxCachedRecs = 0)
		val grouped = new RecommendPipeline(P_In_G, maxCachedRecs = 0)
		
		//Some basic filters
		val NotInputPaper = Not(InputEqualsOutput())
		
		//Recommend papers based on authors
		val papersByAuthors =
			Select(Paper,
				From(authored)
					Where NotInputPaper)
		
		//Recommend papers based on what they reference
		val papersByReference =
			Select(Paper,
				From(referenced)
					Where NotInputPaper)
		
		//Recommend papers based on tags
		val papersByTag =
			Select(Paper,
				From(tagged)
					Where NotInputPaper)
		
		//Recommend papers based on categories
		val papersByCat =
			Select(Paper,
				From(categorized)
					Where NotInputPaper)
		
		//Recommend papers based on their publisher's group
		val papersByGroup =
			Select(Paper,
				From(grouped)
					Where NotInputPaper)
		
		//Recommend papers based on any and all available info
		val papersByAll =
			Select(Paper,
				From(papersByAuthors) Join papersByReference Join papersByCat Join papersByTag Join papersByGroup)
		
		//Recommend file attachments based on a paper
		val InputPaperDoesNotHaveFile = Not(InputRelatedBy(attached.items))
		val filesByAll =
			Select(File,
				From(papersByAll) To attached.items
					Where InputPaperDoesNotHaveFile)
		
		//Recommend categories based on a paper
		val InputPaperDoesNotHaveCat = Not(InputRelatedBy(categorized.items))
		val catsByAll =
			Select(Category,
				From(papersByAll) To categorized.items
					Where InputPaperDoesNotHaveCat)
		
		//Recommend categories based on a paper
		val InputPaperDoesNotHaveTag = Not(InputRelatedBy(tagged.items))
		val tagsByAll =
			Select(Tag,
				From(papersByAll) To tagged.items
					Where InputPaperDoesNotHaveTag)
		
		//Recommend groups similar to a paper
		val InputPaperDoesNotHaveGroup = Not(InputRelatedBy(grouped.items))
		val groupsByAll =
			Select(Group,
				From(papersByAll) To grouped.items
					Where InputPaperDoesNotHaveGroup)
		
		//Recommend authors similar to a paper
		val InputPaperDoesNotHaveAuthor = Not(InputRelatedBy(authored.items))
		val authorsByAll =
			Select(Author,
				From(papersByAll) To authored.items
					Where InputPaperDoesNotHaveAuthor)
		
		def buildComputationGraph() = {
			ComputationGraph(
				Seq(instance.authored, attached, referenced, tagged, categorized, grouped),
				
				triggeredOutputs = Seq(
					
					//Offer recommendations through HTTP interface
					new HttpQuery(papersByAuthors, HttpProtocol("by_authors")),
					new HttpQuery(papersByCat, HttpProtocol("by_cats")),
					new HttpQuery(papersByReference, HttpProtocol("by_refs")),
					new HttpQuery(papersByTag, HttpProtocol("by_tags")),
					new HttpQuery(papersByAll, HttpProtocol("by_all")),
					
					new HttpQuery(filesByAll, HttpProtocol("for_files")),
					new HttpQuery(catsByAll, HttpProtocol("for_cats")),
					new HttpQuery(tagsByAll, HttpProtocol("for_tags")),
					new HttpQuery(groupsByAll, HttpProtocol("for_groups")),
					new HttpQuery(authorsByAll, HttpProtocol("for_authors"))
				)
			)
		}
	}
	
}

