package com.lmi.util

import com.lmi.engine.worker.util.ignite.IgniteConfig
import org.apache.ignite.Ignite
import org.junit.{After, AfterClass}

class IgnitableTest {
	
	implicit val ignite: Ignite = IgniteConfig.getOrCreateIgnite("127.0.0.1")
	
	//Clear all Ignite state after each test
	@After
	//@Before
	def after(): Unit = {
		val names = ignite.cacheNames()
		ignite.destroyCaches(names)
	}
	
	object IgnitableTests {
		@AfterClass
		def teardown(): Unit = {
			val names = ignite.cacheNames()
			ignite.destroyCaches(names)
			ignite.close()
			
		}
	}
	
}
