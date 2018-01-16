package com.lmi.examples.fakeyoutoob

import com.lmi.engine.graph.{Edge, Node, Relation}
import com.lmi.engine.worker.event.EventType
import com.lmi.engine.worker.output.stream.mem.InMemoryOutputStream

object FakeYouToobGraph {
	
	//Relationships between Nodes
	val U_Watched_V = Relation(User, Watched, Video)
	val U_SubscribedTo_U = Relation(User, Subscribed, User)
	val U_Published_V = Relation(User, Published, Video)
	val U_LocatedIn_L = Relation(User, Located, Location)
	val U_Wrote_C = Relation(User, Wrote, Comment)
	val C_Annotated_V = Relation(Comment, Annotated, Video)
	//Output Streams
	val watchRecStream = new InMemoryOutputStream[User.type, User.type]
	val subscribeRecStream = new InMemoryOutputStream[User.type, User.type]
	
	//Event Types
	object SUBSCRIBES extends EventType("Subscribes")
	
	object GET_SUBSCRIBERS_BY_SUBSCRIBEES extends EventType("Subscribees")
	
	object GET_SUBSCRIBERS_BY_KNOWS extends EventType("ByKnows")
	
	object LOCATION_ADDED_EVENT extends EventType("ContactAdded")
	
	object SUBSCRIBER_ADDED_EVENT extends EventType("SubscriberAdded")
	
	//Types of Objects (Things to recommend or base on)
	object User extends Node
	
	object Video extends Node
	
	object Location extends Node
	
	object Comment extends Node
	
	//Types of edges that connect nodes
	object Watched extends Edge
	
	object Subscribed extends Edge
	
	object Published extends Edge
	
	object Located extends Edge
	
	object Wrote extends Edge
	
	object Annotated extends Edge
	
}
