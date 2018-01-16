package com.lmi.engine

import com.lmi.engine.graph.Node
import com.lmi.engine.worker.PipelineContext
import com.lmi.engine.worker.event.{EventDequeuer, QueuedEventIgniteUtil}
import com.lmi.engine.worker.util.ignite.IgniteConfig
import org.apache.ignite.Ignite
import org.apache.ignite.lang.IgniteRunnable

object Engine {
	
	def start
	(app: FreqlApp,
	 tuning: EngineTuning,
	 igniteFactory: () => Ignite = () => IgniteConfig.getOrCreateIgnite(),
	 apiConfigFactory: () => Option[APIConfig] = () => None
	): EngineContext = {
		
		broadcastAppToWorkers(app, tuning, igniteFactory, apiConfigFactory)
	}
	
	//Serialize the app.app to the cluster and establish clean Ignite state
	private def broadcastAppToWorkers(
		                                 app: FreqlApp,
		                                 tuning: EngineTuning,
		                                 localIgniteFactory: () => Ignite,
		                                 apiConfigFactory: () => Option[APIConfig]): EngineContext = {
		
		val ignite = localIgniteFactory()
		ignite.compute.broadcast(new IgniteRunnable {
			override def run(): Unit = {
				val localIgnite = localIgniteFactory()
				
				implicit val localContext = EngineContext(app, localIgnite, tuning)
				
				//Setup Ignite services and pipelines
				val graph = app.buildComputationGraph()
				for (pipeline <- graph.pipelines) {
					pipeline.start()
				}
				
				val apiConfig = apiConfigFactory()
				if(apiConfig.isDefined) {
					val api = apiConfig.map(new HttpApi(_, graph.triggeredOutputs))
					api.map(_.start())
				}
				
				//Start all input streams into Ignite event queue
				val eventSources = app.eventSources
				eventSources.inputs.foreach(_.startStreaming(localIgnite, QueuedEventIgniteUtil.getCacheName(localIgnite)))
				
				//Start polling events from Ignite event queue
				val dequeuer = new EventDequeuer(eventSources.router)
				dequeuer.ensureIsDequeuing()
			}
		})
		
		EngineContext(app, ignite, tuning)
	}
	
}

sealed case class EngineContext
(app: FreqlApp, ignite: Ignite, tuning: EngineTuning) extends PipelineContext {
	
	override def maxObjCount(objType: Node): Int =
		app.maxObjCount(objType)
	
}

