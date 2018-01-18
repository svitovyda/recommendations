package com.svitovyda.recommendations

import com.svitovyda.recommendations.Engine.{Article, Result}
import play.api.libs.json.JsValue


case class Engine(articles: Map[String, Map[String, String]]) {

  /**
    * Returns recommendations based on requested article attributes
    * @param sku an SKU of requested article
    * @param count amount of recommended articles
    * @return `Some(List({SKUs}))` if requested SKU exists, `None` otherwise
    */
  def getRecommendations(sku: String, count: Int = 10): Option[List[String]] = {
    articles.get(sku).map { source ⇒
      articles.foldLeft(Result(List(), Engine.minimalRank(source.size), count)) {
        case (result, (key, target)) ⇒
          if(key == sku) result
          else result + Article(key, Engine.rank(source, target))
      }.recommendations.map(_.sku)
    }
  }
}

object Engine {
  def apply(content: JsValue): Engine = {
    Engine(content.as[Map[String, Map[String, String]]])
  }

  /**
    * Assume the amount and names of attributes can be different,
    * so each attribute that exist in both articles - should already increase the rank.
    * If attribute has same value - its increase should be significant, so that
    * no amount of attributes with same name but different values can give bigger rank
    * than the attribute with same name/value.
    *
    * @param source - article, for which looking for the recommendations
    *
    * @param target - article, that has to be measured if it can be recommended
    *
    * One `unit` - amount of attributes in source article. It is used to avoid possible
    * issues with "magical" numbers that can appear too small for bigger amount of attributes
    *
    * Every attribute that has same name and value - `equalsWeight()` (`unit³`)
    * Every attribute that exists for both SKUs - `unit`
    * Each attribute weight then corrected by 'alphabetical' rank - exponential function
    *
    **/
  def rank(source: Map[String, String], target: Map[String, String]): Double = {
    val unit = source.size.toDouble
    val equalsUnit = equalsWeight(source.size)

    def weight(value1: String, value2: String) =
      if(value1 == value2) equalsUnit
      else unit

    val (rank, _) = source.keys.toList.sorted.foldLeft(0.0, 0) {
      case ((aggregated, index), key) ⇒
        val currentWeight = target.get(key).map{ value ⇒
          alphabetical(weight(value, source(key)), index)
        }.getOrElse(0.0)

        (aggregated + currentWeight, index + 1)
    }
    rank
  }

  /**
    * One global implementation of the weight for matching by name and value attribute
    * @param length size of all attributes in requested article. used as a main scale for
    *               the ranking calculation
    * @return weight for attribute that matches by value
    */
  private def equalsWeight(length: Int) = Math.pow(length, 3)

  /**
    * Returns the weight of the attribute corrected according its alphabetical order
    * @param weight rank of the attribute according to its name/value matching
    * @param index alphabetical index
    * @return corrected rank of the attribute
    */
  private def alphabetical(weight: Double, index: Int): Double = weight * Math.pow(0.9, index)

  /**
    * the weight of matching attribute if it is on the last position
    * @param length - size of the requested article attributes map
    * @return smallest possible rank for matching attribute
    */
  def minimalRank(length: Int): Double = alphabetical(equalsWeight(length), length - 1)

  /**
    * To recommend an article at least one attribute value should match
    * @param source - Map of attributes for requested article
    * @param rank - rank of potential recommendation
    * @return Boolean, is rank big enough
    */
  def isRankEnough(source: Map[String, String], rank: Double): Boolean =
    rank > minimalRank(source.size)


  case class Article(sku: String, rank: Double)
  implicit val ord: Ordering[Article] = Ordering.by[Article, Double](a ⇒ a.rank)

  case class Result(recommendations: List[Article], minRate: Double, count: Int = 10) {
    require(count > 0)

    def +(item: Article): Result = {
      if(item.rank < minRate) this
      else {
        if(recommendations.length < count)
          copy(recommendations = item :: recommendations)
        else {
          val min = recommendations.min

          if(item.rank > min.rank) {
            val updated = item :: recommendations.filter(_ != min)
            copy(recommendations = updated, minRate = updated.min.rank)
          }
          else copy(minRate = min.rank)
        }
      }

    }
  }

}
