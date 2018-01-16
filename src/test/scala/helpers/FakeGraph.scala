package helpers

import com.lmi.engine.graph.{Edge, Id, Node, Relation}

object FakeGraph {
	
	//Fake Relations
	val FooToFoo = Relation(Foo, ToEdge, Foo)
	val FooToBar = Relation(Foo, ToEdge, Bar)
	val FooToPho = Relation(Foo, ToEdge, Pho)
	
	//Fake Obj Id instances
	val idFoo = Id[Foo.type]("1")
	val idFoo2 = Id[Foo.type]("2")
	val idBar = Id[Bar.type]("1")
	val idBar2 = Id[Bar.type]("2")
	val idPho1 = Id[Pho.type]("1")
	
	//Fake Obj Classes
	object Foo extends Node
	
	object Bar extends Node
	
	object Pho extends Node
	
	//Fake Edge Classes
	object ToEdge extends Edge
	
	object ToEdge2 extends Edge
	
}
