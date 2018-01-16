package com.lmi.engine.freql.join

import com.lmi.engine.freql.score.{TopScoreSource, TopScores}
import com.lmi.engine.freql.util.FreqlUtils
import com.lmi.engine.graph._

class JoinedScoreSource
[IN <: Node, T <: Edge, T2 <: Edge, TObj <: Node, GIVEN <: Node, GIVEN2 <: Node]
(source1: TopScoreSource[IN, T, TObj, GIVEN],
 source2: TopScoreSource[IN, T2, TObj, GIVEN2],
 reduceScoreKeysOp: JoinByKeyOp)
	extends TopScoreSource[IN, MergedEdge[T, T2], TObj, MergedNode[GIVEN, GIVEN2]] {
	
	override def getRecommendations(originId: Id[IN], maxCount: Int): TopScores[IN, TObj] = {
		val output1 = source1.getRecommendations(originId, maxCount)
		val output2 = source2.getRecommendations(originId, maxCount)
		
		FreqlUtils.normalize(reduceScoreKeysOp.join(output1, output2))
	}
	
}
