package net.wiringbits.sdb.detectors

import net.wiringbits.sdb.TeamMember
import org.apache.commons.text.similarity.LevenshteinDistance

/**
 * Just a simple way to find how similar are two members by using the levenshtein distance.
 * @param members the trusted members to compare potential scammers to
 */
class SimilarMembersDetector(members: List[TeamMember]) {

  // Similarities smaller that this value lead to potential scammers
  // Higher values means slower runtime, and leads to most matches but anything above 3
  // is likely very annoying.
  private val DistanceThreshold = 1
  private val distance = new LevenshteinDistance(DistanceThreshold)

  private def test(member: String, other: String) = {
    val x = distance(member, other)
    x >= 0 && x <= DistanceThreshold
  }

  def findSimilarMember(username: String): Option[SimilarTeamMember] = {
    val normalizedUsername = normalize(username)
    members.flatMap { team =>
      val minDistance = (distance(normalize(team.raw.user.username), normalizedUsername) :: team.raw.nick
        .map(x => distance(normalize(x), normalizedUsername))
        .toList).min

      if (minDistance == 0) Some(SimilarTeamMember(exactMatch = true, team))
      else if (minDistance > 0 && minDistance <= DistanceThreshold) Some(SimilarTeamMember(exactMatch = false, team))
      else None
    }.headOption
  }

  /**
   * For now, just lower case and take everything between the character 20 and 255,
   * ignoring anything else.
   */
  private def normalize(username: String): String = {
    username.toLowerCase.filter(c => c >= 20 && c <= 255)
  }
}

case class SimilarTeamMember(exactMatch: Boolean, teamMember: TeamMember)
