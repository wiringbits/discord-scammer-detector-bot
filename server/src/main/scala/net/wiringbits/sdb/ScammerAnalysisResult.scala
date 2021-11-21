package net.wiringbits.sdb

sealed trait ScammerAnalysisResult

object ScammerAnalysisResult {
  final case object NoMatches extends ScammerAnalysisResult
  final case class SimilarTeamMemberFound(teamMember: TeamMember) extends ScammerAnalysisResult
  final case class ForbiddenWordFound(word: String) extends ScammerAnalysisResult
}
