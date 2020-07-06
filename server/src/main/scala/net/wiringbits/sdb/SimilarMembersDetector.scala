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
    val normalizedUsername = normalize(username)
    members.find { team =>
      test(normalize(team.raw.user.username), normalizedUsername) ||
      team.raw.nick.exists(x => test(normalize(x.toLowerCase), normalizedUsername))
    }
  }

  /**
   * For now, just lower case and take everything between the character 20 and 255,
   * ignoring anything else.
   */
  private def normalize(username: String): String = {
    username.toLowerCase.filter(c => c >= 20 && c <= 255)
  }
}
