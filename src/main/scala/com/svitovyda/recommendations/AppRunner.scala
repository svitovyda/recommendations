package com.svitovyda.recommendations

import scala.util.Try


object AppRunner {

  def main(args: Array[String]): Unit = args match {
    case Array(filename) =>
      val readingResult = for {
        js ← JsonFileReader.readJson(filename)
        engine ← Try(Engine(js))
      } yield engine

      readingResult.map { engine: Engine ⇒
        InputProcessor.startConsole { sku: String =>
          engine.getRecommendations(sku) match {
            case None ⇒ println(s"The given SKU $sku could not be found")
            case Some(List()) ⇒ println("This article has no good recommendations, similar to it")
            case Some(list) ⇒ println(list.mkString("\n"))
          }
        }
      }.recover { case e: Exception ⇒
        println(s"Error processing the file $filename: ${e.getMessage}")
      }

    case _ => println("Could not parse the file name, try again!")
  }
}
