package net.wiringbits.sdb

import org.apache.commons.text.similarity.LevenshteinDistance

class SimilarMembersDetector(members: List[TeamMember]) {

  private val DistanceThreshold = 2
  private val distance = new LevenshteinDistance(DistanceThreshold)

  private def test(member: String, other: String): Boolean = {
    val x = distance(member, other)
    x >= 0 && x <= DistanceThreshold
  }

  def findSimilarMember(username: String): Option[TeamMember] = {
    val lowerCaseUsername = username.toLowerCase
    members.find { team =>
      test(team.raw.user.username.toLowerCase, lowerCaseUsername) ||
      team.raw.nick.exists(x => test(x.toLowerCase, lowerCaseUsername))
    }
  }
}
