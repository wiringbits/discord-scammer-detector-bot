package net.wiringbits.sdb

import ackcord.data.User

object BotMessage {

  def similarTo(scammerAnalysisResult: ScammerAnalysisResult): String = {
    scammerAnalysisResult match {
      case ScammerAnalysisResult.ForbiddenWordFound(word) => word
      case ScammerAnalysisResult.SimilarTeamMemberFound(teamMember, _) => teamMember.raw.user.username
    }
  }

  def scammerBanned(scammer: User, scammerAnalysisResult: ScammerAnalysisResult.SimilarTeamMemberFound): String = {
    s"Potential scammer banned! ${scammer.mention} looks very similar to our team member ${scammerAnalysisResult.teamMember.raw.user.mention}"
  }

  def scammerNeedsToBeVerified(scammer: User, scammerAnalysisResult: ScammerAnalysisResult): String = {
    scammerAnalysisResult match {
      case ScammerAnalysisResult.ForbiddenWordFound(word) =>
        s"Potential scammer needs to be reviewed manually: ${scammer.mention} has a risky keyword in its name: $word"
      case ScammerAnalysisResult.SimilarTeamMemberFound(teamMember, _) =>
        s"Potential scammer needs to be reviewed manually: ${scammer.mention} looks very similar to our team member ${teamMember.raw.user.mention}"
    }
  }

  def everyoneScammerNeedsToBeVerified(scammer: User, scammerAnalysisResult: ScammerAnalysisResult): String = {
    scammerAnalysisResult match {
      case ScammerAnalysisResult.ForbiddenWordFound(word) =>
        s"@everyone Potential scammer needs to be reviewed manually: ${scammer.mention} has a risky keyword in its name: $word"
      case ScammerAnalysisResult.SimilarTeamMemberFound(teamMember, _) =>
        s"@everyone Potential scammer needs to be reviewed manually: ${scammer.mention} looks very similar to our team member ${teamMember.raw.user.mention}"
    }
  }

}
