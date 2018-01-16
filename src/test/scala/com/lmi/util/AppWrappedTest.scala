package com.lmi.util

import com.lmi.engine
import com.lmi.engine.{Engine, EngineContext, TestTuning}

//Auto clears ignite caches after every test
class AppWrappedTest[A <: engine.FreqlApp](val app: A) extends IgnitableTest {
	
	implicit val context: EngineContext = Engine.start(app, TestTuning(), () => ignite)
	
}
