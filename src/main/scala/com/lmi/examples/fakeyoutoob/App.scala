package com.lmi.examples.fakeyoutoob

import com.lmi.engine._
import com.lmi.engine.freql.map.filter.{InputEqualsOutput, InputRelatesTo, Not}
import com.lmi.engine.freql.{From, Select}
import com.lmi.engine.graph._
import com.lmi.engine.worker.RelationService
import com.lmi.engine.worker.event.EventRouter
import com.lmi.engine.worker.input.EventStream

// A fake example of how YouToob might use Freql Receommendations
class FakeYouToobApp(inputs: Seq[EventStream] = Seq()) extends FreqlApp {
	
	import FakeYouToobGraph._
	
	//Create Similarity pipelines
	val subscribed = new RelationService(U_SubscribedTo_U)
	val located = new RelationService(U_LocatedIn_L)
	val published = new RelationService(U_Published_V)
	val watched = new RelationService(U_Watched_V)
	val commented = new RelationService(U_Wrote_C)
	val described = new RelationService(C_Annotated_V)
	
	//Define some filters
	val NotMe = Not(InputEqualsOutput())
	val NotMeAndNotSubscribedTo = Not(InputEqualsOutput() Or InputRelatesTo(subscribed))
	val NotWatched = Not(InputRelatesTo(watched))
	
	// Get all users who are similar to me based on any user-centric data
	val relatedUsers =
		Select(User,
			From(located) Join subscribed Join watched
				Where NotMe)
	
	// Recommend autoplay published based on everything
	val autoplaySuggestions =
		Select(Video,
			From(relatedUsers) To watched
				Where NotWatched)
	
	// Get the publishers that people similar to me subscribe to, for whom I haven't subscribed.
	val recommendedPublishers =
		Select(User,
			From(relatedUsers) To subscribed
				Where NotMeAndNotSubscribedTo
		)
	
	override def name(): String = "FakeYouToob"
	
	//TODO
	override def inputEvents: EventInputs = {
		val router = new EventRouter(Seq(
			//new SubjectTargetProcessor(SUBSCRIBER_ADDED_EVENT, U_SubscribedTo_U),
			//new SubjectTargetProcessor(SUBSCRIBER_ADDED_EVENT, U_SubscribedTo_U)
		))
		
		EventInputs(inputs, router)
	}
	
	override def outputs: Outputs = {
		Outputs()
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
	override def pipelines(): Seq[RelationService[_ <: Node, _ <: Edge, _ <: Node]] = {
		Seq(subscribed, located, published, watched, commented, described)
	}
	
}

