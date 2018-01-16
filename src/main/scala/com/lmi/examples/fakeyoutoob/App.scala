package com.lmi.examples.fakeyoutoob

import com.lmi.engine._
import com.lmi.engine.freql.map.filter.{InputEqualsOutput, InputRelatedBy, Not}
import com.lmi.engine.freql.{From, Select}
import com.lmi.engine.graph._
import com.lmi.engine.worker.event.EventRouter
import com.lmi.engine.worker.input.EventStream
import com.lmi.engine.worker.{PipelineContext, RecommendPipeline}

// A fake example of how YouToob might use Freql Receommendations
class FakeYouToobApp(inputs: Seq[EventStream] = Seq()) extends FreqlApp {
	
	import FakeYouToobGraph._
	
	override def name(): String = "FakeYouToob"
	
	//TODO
	override def eventSources: EventSources = {
		val router = new EventRouter(Seq(
			//new SubjectTargetProcessor(SUBSCRIBER_ADDED_EVENT, U_SubscribedTo_U),
			//new SubjectTargetProcessor(SUBSCRIBER_ADDED_EVENT, U_SubscribedTo_U)
		))
		
		EventSources(inputs, router)
	}
	
	//Old objs will get cycled out after max count
	override def maxObjCount(objType: Node): Int = {
		objType match {
			case User => 5000
			case Video => 5000
			case _ =>
				require(false, "No max count defined for " + objType.name)
				0
		}
	}
	
	//Build the App-specific pipelines
	override def buildComputationGraph()(implicit context: PipelineContext): ComputationGraph = {
		
		//Create Similarity pipelines
		val subscribed = new RecommendPipeline(U_SubscribedTo_U)
		val located = new RecommendPipeline(U_LocatedIn_L)
		val published = new RecommendPipeline(U_Published_V)
		val watched = new RecommendPipeline(U_Watched_V)
		val commented = new RecommendPipeline(U_Wrote_C)
		val described = new RecommendPipeline(C_Annotated_V)
		
		//Define some filters
		val NotMe = Not(InputEqualsOutput())
		val NotMeAndNotSubscribedTo = Not(InputEqualsOutput() Or InputRelatedBy(subscribed.items))
		val NotWatched = Not(InputRelatedBy(watched.items))
		
		// Get all users who are similar to me based on any user-centric data
		val relatedUsers =
			Select(User,
				From(located) Join subscribed Join watched
					Where NotMe)
		
		// Recommend autoplay published based on everything
		val autoplaySuggestions =
			Select(Video,
				From(relatedUsers) To watched.items
					Where NotWatched)
		
		// Get the publishers that people similar to me subscribe to, for whom I haven't subscribed.
		val recommendedPublishers =
			Select(User,
				From(relatedUsers) To subscribed.items
					Where NotMeAndNotSubscribedTo
			)
		
		//Map events that are requests for recs to a QueryExporter
		//TODO
		
		ComputationGraph(List(subscribed, located, published))
	}
	
}

