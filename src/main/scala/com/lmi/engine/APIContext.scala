package com.lmi.engine

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.lmi.engine.worker.output.trigger.protocols.TriggeredHttp.HttpQuery
import com.lmi.engine.worker.output.trigger.{TriggeredQuery, TriggeredRequest, TriggeredResponse, TriggeredResponseScore}
import spray.json.DefaultJsonProtocol

class APIConfig(val host: String = "localhost", val port: Int = 80)

trait TriggeredJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
	implicit val itemFormat = jsonFormat2(TriggeredRequest)
	implicit val scoreFormat = jsonFormat2(TriggeredResponseScore)
	implicit val responseFormat = jsonFormat1(TriggeredResponse)
}

object APIHelper extends TriggeredJsonSupport {
	
	def createRoutes(httpStreams: Seq[TriggeredQuery[_, _, _]]): Route = {
		var routes = path("") {
			get {
				complete {
					"Use GET '/<Your_URL>' to query this API"
				}
			}
		}
		
		httpStreams.foreach {
			case http: HttpQuery[_, _] => {
				routes = routes ~ path(http.protocol.url) {
					//get {
					parameters('id, 'limit) { (id, limit) =>
						val request = TriggeredRequest(id, limit.toInt)
						complete {
							http.respond(request)
						}
					}
					//}
				}
			}
		}
		
		routes
	}
}

class HttpApi(config: APIConfig, streams: Seq[TriggeredQuery[_, _, _]]) {
	
	//Define all implicits needed for Akka API support
	implicit val system = ActorSystem()
	implicit val materializer = ActorMaterializer()
	implicit val executionContext = system.dispatcher
	
	def start(): Unit = {
		val route = APIHelper.createRoutes(streams)
		
		if(streams.nonEmpty) {
			Http().bindAndHandle(route, config.host, config.port)
		}
	}
	
}
