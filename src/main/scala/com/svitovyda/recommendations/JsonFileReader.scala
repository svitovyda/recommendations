package com.svitovyda.recommendations

import java.io.FileInputStream

import play.api.libs.json.{Json, JsValue}

import scala.util.Try


object JsonFileReader {

  def readJson(filename: String): Try[JsValue] = {
    println(s"Reading file: $filename")

    val file = Try(new FileInputStream(filename))
    val js = for {
      stream ← file
      js ← Try(Json.parse(stream))
    } yield js
    file.foreach(_.close())
    js
  }
}
