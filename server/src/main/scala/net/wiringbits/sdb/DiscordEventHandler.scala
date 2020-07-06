package net.wiringbits.sdb

import ackcord.data.{GuildId, User, UserId}
import ackcord.{APIMessage, CacheSnapshot}
import net.wiringbits.sdb.config.DiscordServerConfig
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
 * Processes the events produced by discord, we are particularly interested on:
 * - Members joining
 * - Members updated
 * - The client is ready
 */
class DiscordEventHandler(
    discordAPI: DiscordAPI,
    sharedState: SharedState
)(
    implicit ec: ExecutionContext
) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def handler()(implicit c: CacheSnapshot): PartialFunction[APIMessage, Unit] = {
    case _: APIMessage.Ready => initialize()
    case e: APIMessage.GuildCreate => logger.info(s"Bot installed on ${e.guild.name}")

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

  // When the client is ready, initialize the shared state, so that we sync the config with discord.
  private def initialize()(implicit c: CacheSnapshot): Unit = {
    logger.info("Client ready, initializing")

    def getDetails(guildId: GuildId, config: DiscordServerConfig) = {
      discordAPI
        .getNotificationChannel(guildId, config)
        .flatMap {
          case None => throw new RuntimeException(s"Missing notification channel for guild = $guildId")
          case Some(channel) => getMembers(guildId, config.members).map(SharedState.ServerDetails(channel, _))
        }
    }

    val result = for {
      guilds <- discordAPI.getSupportedGuilds
      guildsChannelF = guilds.map {
        case (config, guild) =>
          getDetails(guild.id, config)
      }
      guildChannels <- Future.sequence(guildsChannelF)
    } yield {
      guildChannels.foreach { channel =>
        val members = channel.members.map(_.raw.user.username).mkString(" | ")
        logger.info(s"Members for guild = ${channel.notificationChannel.name}, synced: $members")
        sharedState.add(channel)
      }
    }

    result.onComplete {
      case Success(_) => ()
      case Failure(ex) =>
        logger.warn(s"Failed to initialize", ex)
        sys.exit(1)
    }
  }

  /**
   * For every new user or user updated, let's find whether it's a potential scammer, the rules are:
   * - If the user is one of the configured members, do nothing.
   * - Check the Levenshtein Distance from the user (nickname, username) against every configured
   *   member (nickname, username), notify to the configured channel if we find it to be quite similar.
   */
  private def handleUser(channel: SharedState.ServerDetails, user: User, nick: Option[String])(
      implicit c: CacheSnapshot
  ): Unit = {
    if (channel.members.exists(_.raw.user.id == user.id)) {
      // a trusted member can change it's nickname
      ()
    } else {
      val similarMembersDetector = new SimilarMembersDetector(channel.members)
      similarMembersDetector
        .findSimilarMember(user.username)
        .orElse { nick.flatMap(similarMembersDetector.findSimilarMember) }
        .foreach { relatedTeamMember =>
          logger.info(
            s"Found potential scammer: ${user.username}, nick = $nick, similar to ${relatedTeamMember.raw.user.username}"
          )
          handlePotentialScammer(
            channel,
            relatedTeamMember = relatedTeamMember,
            scammerMention = user.mention
          )
        }
    }
  }

  /**
   * For now just send a notification to the configured channel about the potential scammer
   */
  private def handlePotentialScammer(
      channel: SharedState.ServerDetails,
      scammerMention: String,
      relatedTeamMember: TeamMember
  )(implicit c: CacheSnapshot): Unit = {
    val msg =
      s"Potential scammer: $scammerMention looks very similar to our team member ${relatedTeamMember.raw.user.mention}"
    discordAPI.sendMessage(channel.notificationChannel, msg)
  }

  /**
   * Given the member ids for a guild, let's sync their data from discord, so that we know their
   * usernames and nicknames while looking for potential scammers.
   */
  private def getMembers(guildId: GuildId, ids: List[String])(implicit c: CacheSnapshot): Future[List[TeamMember]] = {
    val f = ids
      .flatMap { id =>
        Try(UserId(id)).toOption
      }
      .map { id =>
        val userId = UserId(id)
        discordAPI
          .getMember(guildId, userId)
          .recover {
            case NonFatal(ex) =>
              // for now, just log unresolved members, likely the config needs to be updated
              logger.warn(s"Can't resolve member = $id, guild = $guildId", ex)
              None
          }
      }

    Future.sequence(f).map(_.flatten).map { list =>
      list.map(raw => TeamMember(raw))
    }
  }
}
