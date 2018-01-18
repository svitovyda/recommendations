package com.svitovyda.recommendations

import scala.annotation.tailrec


object InputProcessor {

  private val skuPattern = "([\\d\\w-]{3,})".r
  private val quitPattern = "([qQ])".r

  def startConsole(f: String => Unit): Unit = {

    def readNextCommand(): Option[String] = {
      print("Enter an SKU: ")
      scala.io.StdIn.readLine() match {
        case quitPattern(_) => None
        case skuPattern(sku) => Some(sku)
        case _ => Some("")
      }
    }

    @tailrec
    def loop(): Unit = readNextCommand() match {
      case Some("") =>
        println("Could not parse your command. You can enter 'q' to quit.")
        loop()
      case Some(sku) =>
        f(sku)
        loop()
      case None => println("See ya! :)")
    }

    println("You can now enter the SKU of article and get the recommendations. To exit type 'q'")
    loop()
  }

}
