package net.wiringbits.sdb.detectors

import org.apache.commons.text.similarity.LevenshteinDistance

/**
 * A simple way to find how similar are a forbidden word and a member by using the levenshtein distance.
 * @param forbiddenWords the words from config to compare usernames to
 */
class ForbiddenWordsDetector(forbiddenWords: List[String]) {
  val DistanceThreshold = 1
  val distance = new LevenshteinDistance(DistanceThreshold)

  def findMatch(username: String): Option[ForbiddenWordMatched] = {
    val normalizedUsername = normalize(username)
    val (word, minDistance) = compareWordInUsername(normalizedUsername)
    val (forbiddenWord, containsForbiddenWord) = checkIfContains(username)

    if (minDistance >= 0 && minDistance <= DistanceThreshold) {
      Some(ForbiddenWordMatched(word))
    } else if (containsForbiddenWord) {
      Some(ForbiddenWordMatched(forbiddenWord))
    } else {
      None
    }
  }

  private def compareWordInUsername(normalizedUsername: String): (String, Int) = {
    forbiddenWords
      .map(x => (x, distance(normalize(x), normalizedUsername).intValue))
      .max
  }

  private def checkIfContains(normalizedUsername: String): (String, Boolean) = {
    forbiddenWords.map(x => (x, normalizedUsername.contains(x))).find(_._2 == true).getOrElse("", false)
  }

  private def normalize(username: String): String = {
    username.toLowerCase.filter(c => c >= 20 && c <= 255)
  }
}

case class ForbiddenWordMatched(word: String)
