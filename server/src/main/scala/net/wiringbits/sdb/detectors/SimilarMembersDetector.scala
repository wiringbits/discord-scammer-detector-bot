package net.wiringbits.sdb.detectors

import net.wiringbits.sdb.TeamMember

/**
 * Just a simple way to find how similar are two members by using the levenshtein distance.
 * @param members the trusted members to compare potential scammers to
 */
class SimilarMembersDetector(members: List[TeamMember]) extends BaseScammerDetector {

  def findPotentialScammer(username: String): Option[SimilarTeamMember] = {
    val normalizedUsername = normalize(username)
    members.flatMap { team =>
      val minDistance = compareSimilarUsername(team, normalizedUsername)

      if (minDistance == 0) {
        Some(SimilarTeamMember(exactMatch = true, teamMember = team))
      } else if (minDistance > 0 && minDistance <= DistanceThreshold) {
        Some(SimilarTeamMember(exactMatch = false, teamMember = team))
      } else {
        None
      }
    }.headOption
  }

  private def compareSimilarUsername(team: TeamMember, normalizedUsername: String): Int = {
    (distance(normalize(team.raw.user.username), normalizedUsername) :: team.raw.nick
      .map(x => distance(normalize(x), normalizedUsername))
      .toList).min
  }
}

case class SimilarTeamMember(exactMatch: Boolean, teamMember: TeamMember) extends BaseScammer
