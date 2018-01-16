package com.lmi.engine.freql.join

case class JoinByMaxOp() extends JoinByKeyOp {
	
	override protected def join(oldScore: Option[Double], score: Double): Option[Double] = {
		Some(
			Math.max(oldScore.getOrElse(Double.MinValue), score)
		)
	}
	
}
