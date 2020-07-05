package net.wiringbits.sdb

import ackcord.data.{GuildChannel, User}
import ackcord.{APIMessage, CacheSnapshot}
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

class DiscordEventHandler(
    discordAPI: DiscordAPI,
    sharedState: SharedState
)(
    implicit ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def handler()(implicit c: CacheSnapshot): PartialFunction[APIMessage, Unit] = {
    case _: APIMessage.Ready => initialize()

    case e: APIMessage.GuildCreate =>
      logger.info(s"Bot installed on ${e.guild.name}")

    case e: APIMessage.GuildMemberAdd =>
      logger.info(s"${e.guild.name} - Member add: nickname = ${e.member.nick}, ${e.member.user.map(_.username)}")
      for {
        channel <- sharedState.findBy(e.guild.id)
        user <- e.member.user
      } yield handleUser(channel, user, e.member.nick)

    case e: APIMessage.GuildMemberUpdate =>
      logger.info(s"${e.guild.name} - Member update: newNickname = ${e.nick}, username = ${e.user.username}")
      sharedState
        .findBy(e.guild.id)
        .foreach { channel =>
          handleUser(channel, e.user, e.nick)
        }
  }

  private def initialize()(implicit c: CacheSnapshot): Unit = {
    logger.info("Client ready, initializing")

    discordAPI.getSupportedGuilds
      .foreach { guilds =>
        guilds.map {
          case (config, guild) =>
            discordAPI
              .getNotificationChannel(guild.id, config)
              .foreach {
                case Some(channel) =>
                  logger.info(s"Got guild: name = ${guild.name}, notifications channel = ${channel.name}")
                  sharedState.addChannel(channel)

                case None =>
                  logger.info(
                    s"Failed to set up guild = ${guild.name}, channel not found = ${config.notificationsChannelName}"
                  )
              }
        }
      }
  }

  private def handleUser(channel: GuildChannel, user: User, nick: Option[String])(
      implicit c: CacheSnapshot
  ): Unit = {
    val similarMembersDetector = new SimilarMembersDetector(sharedState.whitelistedMembers(channel.guildId))
    similarMembersDetector
      .findSimilarMember(user.username)
      .orElse { nick.flatMap(similarMembersDetector.findSimilarMember) }
      .foreach { relatedTeamMember =>
        logger.info(s"Found potential scammer: ${user.username}, nick = $nick, similar to $relatedTeamMember")
        handlePotentialScammer(
          channel,
          relatedTeamMember = relatedTeamMember,
          scammerMention = user.mention
        )
      }
  }

  private def handlePotentialScammer(
      channel: GuildChannel,
      scammerMention: String,
      relatedTeamMember: String
  )(implicit c: CacheSnapshot): Unit = {
    val msg =
      s"Potential scammer: $scammerMention looks very similar to our team member $relatedTeamMember"
    discordAPI.sendMessage(channel, msg)
  }
}
