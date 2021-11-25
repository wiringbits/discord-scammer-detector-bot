package net.wiringbits.sdb

sealed trait ScammerAnalysisResult extends Product with Serializable

object ScammerAnalysisResult {
  final case object NoMatches extends ScammerAnalysisResult
  final case class SimilarTeamMemberFound(teamMember: TeamMember, exactMatch: Boolean) extends ScammerAnalysisResult
  final case class ForbiddenWordFound(word: String) extends ScammerAnalysisResult
}
