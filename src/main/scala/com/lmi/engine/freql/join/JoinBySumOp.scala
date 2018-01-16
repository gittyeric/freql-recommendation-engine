package com.lmi.engine.freql.join

case class JoinBySumOp() extends JoinByKeyOp {
	
	override protected def join(oldScore: Option[Double], score: Double): Option[Double] = {
		Some(
			oldScore.getOrElse(0d) + score
		)
	}
	
}
