package com.lmi.engine.freql.score

import com.lmi.engine.graph.{Id, Node}

case class TopScores[INPUT <: Node, OUTPUT <: Node](
	                                                   originId: Id[INPUT],
	                                                   scores: Seq[Score[OUTPUT]])