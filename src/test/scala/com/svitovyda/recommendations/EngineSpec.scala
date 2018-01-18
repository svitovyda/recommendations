package com.svitovyda.recommendations

import com.svitovyda.recommendations.Engine._
import org.scalatest.{MustMatchers, WordSpecLike}
import play.api.libs.json.Json


class EngineSpec extends WordSpecLike with MustMatchers {

  "apply()" must {

    "create Engine from valid JsValue" in {
      val jsStr = """
        {
        "sku-1": {"a": "a1", "b": "b1", "c": "c1"},
        "sku-2": {"a": "a2", "b": "b1", "c": "c1"},
        "sku-3": {"a": "a1", "b": "b3", "c": "c3"}
        }"""

      val js = Json.parse(jsStr)
      Engine(js).articles.keys.toList must be (List("sku-1", "sku-2", "sku-3"))
    }

    "throw exception on wrong JsValue" in {
      val jsStr = """
        {
        "a": ["a", "a1", "b", "b1", "c", "c1"],
        "b": 3,
        "sku-3": {"a": "a1", "b": "b3", "c": "c3"}
        }"""

      val js = Json.parse(jsStr)

      assertThrows[RuntimeException] {
        Engine(js)
      }
    }
  }

  private val source = Map("a" → "1", "b" → "1", "c" → "1", "d" → "1", "e" → "1")
  private val last   = Map("a" → "0", "b" → "0", "c" → "0", "d" → "0", "e" → "1")

  private def rank(target: Map[String, String]) = Engine.rank(source, target)

  "rank()" must {

    "return consistent result" in {
      rank(source) must be (Engine.rank(last, last))
      rank(last) must be (Engine.rank(last, source))
      rank(Map()) must be (0)
    }

    "ignore attributes that don't match by names" in {
      rank(source) must be (rank(source.updated("aa", "1")))
    }

    "correctly handle alphabetical order" in {
      rank(Map("a" → "0", "b" → "1")) must be < rank(Map("a" → "1", "b" → "0"))
    }

    "matching values must have bigger weight than matching names" in {
      rank(source.map(_._1 → "0")) must be < rank(Map("e" → "1"))
    }

    "matching names must add weight" in {
      rank(Map("a" → "0", "e" → "1")) must be > rank(Map("e" → "1"))
    }

    "matching values must have bigger weight than alphabetical position" in {
      rank(Map("a" → "1")) must be < rank(Map("d" → "1", "e" → "1"))
    }

    "rank correctly if attributes are not in alphabetical order" in {
      val s  = Map("b" → "b-7", "i" → "i-7", "d" → "d-40", "a" → "a-8", "e" → "e-11", "j" → "j-80")
      val e1 = Map("b" → "b-7", "i" → "i-5", "d" → "d-40", "a" → "a-8", "e" → "e-61", "j" → "j-13")
      val e2 = Map("b" → "b-7", "i" → "i-7", "d" → "d-15", "a" → "a-8", "e" → "e-61", "j" → "j-14")

      Engine.rank(s, e1) must be > Engine.rank(s, e2)
    }
  }

  "isRankEnough()" must {
    "allow at least one match" in {
      Engine.isRankEnough(source, rank(last)) must be (true)
    }

    "don't allow without any match" in {
      Engine.isRankEnough(source, rank(last.updated("e", "0"))) must be (false)
    }

    "work with empty or one element map" in {
      Engine.isRankEnough(Map(), rank(last.updated("e", "0"))) must be (true)
      Engine.isRankEnough(Map("a" → "a"), Engine.rank(Map("a" → "a"), Map())) must be (false)
    }
  }

  "minimalRank()" must {
    "correctly work with empty map or one element" in {
      Engine.minimalRank(0) must be (0)
      Engine.minimalRank(1) must be (Engine.rank(Map("a" → "a"), Map("a" → "a")))
    }

    "return correct value for bigger maps" in {
      val test = Map("a1" → "11", "b1" → "11", "c1" → "11", "d1" → "11", "e" → "1")
      Engine.minimalRank(source.size) must be (rank(test))
    }
  }

  "Result" must {
    "correctly add any article with at least one attribute matching" in {
      val result = Result(List(), Engine.minimalRank(source.size), 3)
      val result1 = result + Article("a", rank(last))
      result1.recommendations must have size 1
      val result2 = result1 + Article("b", rank(last))
      result2.recommendations must have size 2
      val result3 = result2 + Article("c", rank(last))
      result3.recommendations must have size 3

      (result2 + Article("d", 0.1)).recommendations must have size 2
    }

    "correctly add article with higher rank and ignore lower ranks" in {
      val result = Result(
        List(Article("a", rank(last)), Article("b", rank(last)), Article("c", rank(last))),
        Engine.minimalRank(source.size), 3)

      val result1 = result + Article("d", rank(last) + 1000)
      result1.minRate must be (rank(last))
      result1.recommendations must have size 3
      result1.recommendations.map(_.sku) must contain ("d")

      val result2 = result1 + Article("f", rank(last) + 1000) + Article("g", rank(last) + 1000)
      result2.recommendations must have size 3
      result2.recommendations.map(_.sku) must be (List("g", "f", "d"))
      result2.minRate must be (rank(last) + 1000)

      result2 + Article("h", rank(last)) must be (result2)
    }
  }

  "getRecommendations" must {
    "return correct list of recommendations for existing sku" in {
      val jsStr = """
        {
        "sku-1": {"at": "a1", "bt": "b1", "ct": "c1"},
        "sku-2": {"at": "a2", "bt": "b1", "ct": "c1"},
        "sku-3": {"at-a": "a1", "at-b": "b3", "at-c": "c3"},
        "sku-4": {"at-a": "a2", "at-b": "b1", "at-c": "c3"},
        "sku-5": {"at-a": "a1", "at-b": "b1", "at-c": "c1"},
        "sku-6": {"a": "a", "b": "b", "c": "c"}
        }"""

      val engine = Engine(Json.parse(jsStr))
      engine.getRecommendations("sku-1") must be (Some(List("sku-2")))
      engine.getRecommendations("sku-2") must be (Some(List("sku-1")))
      engine.getRecommendations("sku-3", 1) must be (Some(List("sku-5")))
      engine.getRecommendations("sku-3") must be (Some(List("sku-5", "sku-4")))
      engine.getRecommendations("sku-6") must be (Some(List()))
    }

    "return nothing if SKU doesn't exist" in {
      val jsStr = """
        {
        "sku-1": {"a": "a1", "b": "b1", "c": "c1"},
        "sku-2": {"a": "a2", "b": "b1", "c": "c1"},
        "sku-3": {"a": "a1", "b": "b3", "c": "c3"},
        "sku-4": {"a": "a1", "b": "b1", "c": "c1"},
        "sku-5": {"a": "a2", "b": "b1", "c": "c1"},
        "sku-6": {"a": "a1", "b": "b3", "c": "c3"}
        }"""

      val engine = Engine(Json.parse(jsStr))
      engine.getRecommendations("abcd") must be (None)
    }
  }

}
