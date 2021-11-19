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
        channel <- sharedState.findBy(e.guild.id).orElse {
          logger.warn(s"No shared state found for guild = ${e.guild.id}")
          None
        }
        user <- e.member.user.orElse {
          logger.warn(s"No user found in the snapshot for id = ${e.member.userId}, nick = ${e.member.nick}")
          None
        }
      } yield handleUser(channel, user, e.member.nick)

    case e: APIMessage.GuildMemberUpdate =>
      logger.info(s"${e.guild.name} - Member update: newNickname = ${e.nick}, username = ${e.user.username}")
      sharedState
        .findBy(e.guild.id)
        .orElse {
          logger.warn(s"No shared state found for guild = ${e.guild.id}")
          None
        }
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
          case Some(channel) =>
            getMembers(guildId, config.members).map(x => SharedState.ServerDetails(channel, x, config.blacklist))
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

        // remove hacky way to trigger the action
        lookForExistingScammers(channel)
      }
    }

    result.onComplete {
      case Success(_) => ()
      case Failure(ex) =>
        logger.warn(s"Failed to initialize", ex)
        sys.exit(1)
    }
  }

  // TODO: Remove hack, when the bot reconnects, we don't need to look for everyone
  private var lookedForExistingScammersIn: Set[GuildId] = Set.empty
  private def lookForExistingScammers(channel: SharedState.ServerDetails)(implicit c: CacheSnapshot): Unit = {
    if (lookedForExistingScammersIn.contains(channel.notificationChannel.guildId)) {
      ()
    } else {
      logger.info(s"Look for existing scammers in the server = ${channel.notificationChannel.guildId}")
      val result = for {
        members <- discordAPI.getAllMembers(channel.notificationChannel.guildId)
      } yield {
        logger.info(
          s"There are ${members.size} members in the analyzed server = ${channel.notificationChannel.guildId}"
        )
        val scammers = members.flatMap { member =>
          findPotentialScammer(channel, member.user, member.nick).map(_ -> member)
        }

        if (scammers.isEmpty) {
          logger.info(
            s"There are no potential scammers in the server = ${channel.notificationChannel.guildId}"
          )
        } else {
          val scammerTextList = scammers.map(_._2.user.username).mkString("[", ",", "]")
          logger.info(
            s"There are ${scammers.size} potential scammers in the server = ${channel.notificationChannel.guildId}, banning them now: $scammerTextList"
          )
        }

        lookedForExistingScammersIn = lookedForExistingScammersIn + channel.notificationChannel.guildId
        scammers.foreach {
          case (relatedTeamMember, scammer) =>
            handlePotentialScammer(
              channel,
              scammer.user,
              relatedTeamMember = relatedTeamMember
            )
        }
      }

      result.onComplete {
        case Success(_) =>
          logger.info("Looked for existing members")
        case Failure(ex) =>
          logger.error(
            s"Failed to look for existing scammers in the server = ${channel.notificationChannel.guildId}",
            ex
          )
      }
    }
  }

  private def findPotentialScammer(
      channel: SharedState.ServerDetails,
      user: User,
      nick: Option[String]
  ): Option[SimilarTeamMember] = {
    if (channel.members.exists(_.raw.user.id == user.id)) {
      // a trusted member can change it's nickname
      None
    } else {
      val potentialScammerDetector = new PotentialScammerDetector(channel.members, channel.blacklist)
      potentialScammerDetector
        .findPotentialScammer(user.username)
        .orElse { nick.flatMap(potentialScammerDetector.findPotentialScammer) }
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
    findPotentialScammer(channel, user, nick)
      .orElse {
        logger.info(s"No matches found for ${user.username}, nick = $nick, id = ${user.id}")
        None
      }
      .foreach { relatedTeamMember =>
        logger.warn(
          s"Found potential scammer: ${user.username}, nick = $nick, id = ${user.id} similar to ${relatedTeamMember.teamMember.raw.user.username}"
        )
        handlePotentialScammer(
          channel,
          user,
          relatedTeamMember = relatedTeamMember
        )
      }
  }

  /**
   * For now just send a notification to the configured channel about the potential scammer
   */
  private def handlePotentialScammer(
      channel: SharedState.ServerDetails,
      scammer: User,
      relatedTeamMember: SimilarTeamMember
  )(implicit c: CacheSnapshot): Unit = {
    def doBan(): Unit = {
      discordAPI.banMember(channel.notificationChannel.guildId, scammer.id).onComplete {
        case Success(_) =>
          val msg =
            s"Potential scammer banned! ${scammer.mention} looks very similar to our team member ${relatedTeamMember.teamMember.raw.user.mention}"
          discordAPI.sendMessage(channel.notificationChannel, msg)

        case Failure(ex) =>
          logger.warn(
            s"Failed to ban potential scammer, guild = ${channel.notificationChannel.guildId}, id = ${scammer.id}, username = ${scammer.username}, relalted to ${relatedTeamMember.teamMember.raw.user.username}",
            ex
          )

          val msg =
            s"Potential scammer needs to be banned manually: ${scammer.mention} looks very similar to our team member ${relatedTeamMember.teamMember.raw.user.mention}"
          discordAPI.sendMessage(channel.notificationChannel, msg)
      }
    }

    if (relatedTeamMember.exactMatch) {
      doBan()
    } else {
      val msg =
        s"@everyone Potential scammer needs to be verified manually: ${scammer.mention} looks very similar to our team member ${relatedTeamMember.teamMember.raw.user.mention}"
      discordAPI.sendMessage(channel.notificationChannel, msg)
    }
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
