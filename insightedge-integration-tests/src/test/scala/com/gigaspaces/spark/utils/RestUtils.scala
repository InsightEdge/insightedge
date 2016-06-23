package com.gigaspaces.spark.utils

import org.scalatest.Assertions._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


/**
  * @author Oleksiy_Dyagilev
  */
object RestUtils {

  def jsonBody(respFuture: Future[WSResponse], timeout: Duration = 1.second): JsValue = {
    val res = Await.result(respFuture, timeout)
    assert(res.status == 200, res)
    println(res.body)
    Json.parse(res.body)
  }
}
