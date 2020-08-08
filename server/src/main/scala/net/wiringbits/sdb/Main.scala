package net.wiringbits.sdb

import ackcord._
import ackcord.gateway.GatewayIntents
import com.typesafe.config.ConfigFactory
import net.wiringbits.sdb.config.DiscordConfig
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object Main extends {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load().getConfig("discord")
    val discordConfig = DiscordConfig(config)

    val clientSettings =
      ClientSettings(discordConfig.token, intents = GatewayIntents.Guilds ++ GatewayIntents.GuildMembers)

    import clientSettings.executionContext

    logger.info("Starting client")
    clientSettings.createClient().onComplete {
      case Success(client) =>
        val discordAPI = new DiscordAPI(discordConfig.whitelistedServersConfig, client)
        val sharedState = new SharedState
        val eventHandler = new DiscordEventHandler(discordAPI, sharedState)
        client.onEventSideEffects { c =>
          eventHandler.handler()(c)
        }
        client.login()

      case Failure(ex) =>
        logger.error("Failed to create the discord client, exiting...", ex)
        sys.exit(1)
    }
  }
}
