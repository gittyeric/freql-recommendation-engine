package com.lmi.util

import com.lmi.engine.worker.util.ignite.IgniteConfig
import org.apache.ignite.Ignite
import org.junit.{After, AfterClass}

class IgnitableTest {
	
	implicit val ignite: Ignite = IgniteConfig.getOrCreateIgnite("127.0.0.1")
	
	//Clear all Ignite state after each test
	@After
	def after(): Unit = {
		val names = ignite.cacheNames()
		ignite.destroyCaches(names)
		
		/*names.forEach(new Consumer[String] {
			override def accept(name: String): Unit = {
				ignite.cache(name).removeAll()
			}
		})*/
	}
	
	object IgnitableTests {
		@AfterClass
		def teardown(): Unit = {
			ignite.close()
		}
	}
	
}
