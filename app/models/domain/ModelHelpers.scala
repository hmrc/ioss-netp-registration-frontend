package models.domain

import scala.annotation.tailrec

object ModelHelpers {

  def normaliseSpaces(string: String): String = {

    @tailrec
    def removeDoubleSpaces(string: String): String = {
      if(!string.contains("  ")) {
        string
      } else {
        removeDoubleSpaces(string.replaceAll("[ ]{2}", " "))
      }
    }

    removeDoubleSpaces(string.trim)
  }

  def normaliseSpaces(string: Option[String]): Option[String] = string.map(normaliseSpaces)

}