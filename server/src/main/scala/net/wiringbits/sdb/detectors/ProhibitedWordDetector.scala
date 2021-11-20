package net.wiringbits.sdb.detectors

/**
 * A simple way to find how similar are two members by using the levenshtein distance.
 * @param blacklist the words from config to compare usernames to
 */
class ProhibitedWordDetector(blacklist: Option[List[String]]) extends BaseScammerDetector {

  def findPotentialScammer(username: String): Option[SimilarToKeyword] = {
    val normalizedUsername = normalize(username)
    val (word, minDistance) = compareWordInUsername(normalizedUsername)

    if (minDistance == 0) {
      Some(SimilarToKeyword(exactMatch = true, keyword = word))
    } else if (minDistance > 0 && minDistance <= DistanceThreshold) {
      Some(SimilarToKeyword(exactMatch = false, keyword = word))
    } else {
      None
    }
  }

  private def compareWordInUsername(normalizedUsername: String): (String, Int) = {
    blacklist
      .map(_.map(x => (x, distance(normalize(x), normalizedUsername).intValue)).max)
      .getOrElse(("", DistanceThreshold + 1))
  }
}

case class SimilarToKeyword(exactMatch: Boolean, keyword: String) extends BaseScammer
