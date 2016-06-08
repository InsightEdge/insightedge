package com.gigaspaces.spark

import com.gigaspaces.spark.utils.{DockerUtils, RestUtils}
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WSResponse

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.http.Writeable._
import play.api.mvc._
import play.api.libs.ws._

/**
  * @author Oleksiy_Dyagilev
  */
object Main extends App {

//  val wsClient = NingWSClient()

//  val resp = wsClient.url(s"http://localhost:32843/api/notebook/interpreter/bind/INSIGHTEDGE-BASIC").get()
//  val res = Await.result(resp, 1.second)
//
//  val json = Json.parse(res.body)
//
//  val ids: Seq[JsValue] = json \\ "id"
////  JsArray(ids)
////  val interpreterIds = ids.map { case JsString(s) => s }
//
//
//
//  val bindResp = wsClient.url(s"http://localhost:32843/api/notebook/interpreter/bind/INSIGHTEDGE-BASIC").put(JsArray(ids))
//
//  println(RestUtils.jsonBody(bindResp))


//  wsClient.url("http://localhost:8080/api/notebook/job/2A94M5J1Z").post()

//  wsClient.close()


  val exec: Int = DockerUtils.dockerExec("f4ec99951f54", "date3")
  println(" res " + exec)



}
