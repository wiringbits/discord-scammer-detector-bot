package net.wiringbits.sdb

import org.apache.commons.text.similarity.LevenshteinDistance

class SimilarMembersDetector(members: List[String]) {

  private val DistanceThreshold = 2
  private val distance = new LevenshteinDistance(DistanceThreshold)

  def findSimilarMember(username: String): Option[String] = {
    val lowerCaseUsername = username.toLowerCase
    members.find { team =>
      val x = distance(team.toLowerCase, lowerCaseUsername)
      x >= 1 && x <= DistanceThreshold
    }
  }
}
