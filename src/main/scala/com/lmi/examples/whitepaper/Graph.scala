package com.lmi.examples.whitepaper

import com.lmi.engine.graph.{Edge, Node, Relation}
import com.lmi.engine.worker.event.EventType

object WhitepaperGraph {
	
	//Relationships between Nodes
	val P_AuthoredBy_A = Relation(Paper, Authored, Author)
	val P_Refs_R = Relation(Paper, Referenced, Reference)
	val P_Has_T = Relation(Paper, Tagged, Tag)
	val P_Has_F = Relation(Paper, Attached, File)
	val P_Has_C = Relation(Paper, Categorized, Category)
	val P_In_G = Relation(Paper, Grouped, Group)
	
	//Event Types
	object AuthoredEvent extends EventType("AuthoredA")
	
	object ReferencedEvent extends EventType("ARefsA")
	
	object TaggedEvent extends EventType("AHasTag")
	
	object AttachedEvent extends EventType("AHasFile")
	
	object CategorizedEvent extends EventType("AHasCat")
	
	object GroupedEvent extends EventType("GroupHasA")
	
	//Node Types
	object Author extends Node
	
	object Paper extends Node
	
	object Category extends Node
	
	object Tag extends Node
	
	object File extends Node
	
	object Reference extends Node
	
	object Group extends Node
	
	//Edge Types
	object Authored extends Edge
	
	object Referenced extends Edge
	
	object Tagged extends Edge
	
	object Attached extends Edge
	
	object Categorized extends Edge
	
	object Grouped extends Edge
	
}
