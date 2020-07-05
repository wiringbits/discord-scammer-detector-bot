package net.wiringbits.sdb

import ackcord.data.{GuildId, User, UserId}
import ackcord.{APIMessage, CacheSnapshot}
import net.wiringbits.sdb.config.DiscordServerConfig
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

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
              logger.warn(s"Can't resolve member = $id, guild = $guildId", ex)
              None
          }
      }
    Future.sequence(f).map(_.flatten).map { list =>
      list.map(raw => TeamMember(raw))
    }
  }

  private def initialize()(implicit c: CacheSnapshot): Unit = {
    logger.info("Client ready, initializing")

    def getDetails(guildId: GuildId, config: DiscordServerConfig) = {
      discordAPI
        .getNotificationChannel(guildId, config)
        .flatMap {
          case None => throw new RuntimeException(s"Missing notification channel for guild = $guildId")
          case Some(channel) => getMembers(guildId, config.members).map(SharedState.Channel(channel, _))
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
        logger.info(s"Members for guild = ${channel.guildChannel.name}, synced: $members")
        sharedState.addChannel(channel)
      }
    }

    result.onComplete {
      case Success(_) => ()
      case Failure(ex) =>
        logger.warn(s"Failed to initialize", ex)
        sys.exit(1)
    }
  }

  private def handleUser(channel: SharedState.Channel, user: User, nick: Option[String])(
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

  private def handlePotentialScammer(
      channel: SharedState.Channel,
      scammerMention: String,
      relatedTeamMember: TeamMember
  )(implicit c: CacheSnapshot): Unit = {
    val msg =
      s"Potential scammer: $scammerMention looks very similar to our team member ${relatedTeamMember.raw.user.mention}"
    discordAPI.sendMessage(channel.guildChannel, msg)
  }
}
