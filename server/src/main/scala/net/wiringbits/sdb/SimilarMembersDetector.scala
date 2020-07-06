package net.wiringbits.sdb

import org.apache.commons.text.similarity.LevenshteinDistance

/**
 * Just a simple way to find how similar are two members by using the levenshtein distance.
 * @param members the trusted members to compare potential scammers to
 */
class SimilarMembersDetector(members: List[TeamMember]) {

  // Similarities smaller that this value lead to potential scammers
  // Higher values means slower runtime, and leads to most matches but anything above 3
  // is likely very annoying.
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
