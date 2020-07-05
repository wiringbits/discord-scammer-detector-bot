package net.wiringbits.sdb

import ackcord._
import com.typesafe.config.ConfigFactory
import net.wiringbits.sdb.config.DiscordConfig
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main extends {

  private val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load().getConfig("discord")
    val discordConfig = DiscordConfig(config)

    val clientSettings = ClientSettings(discordConfig.token)
    import clientSettings.executionContext

    // In real code, please don't block on the client construction
    logger.info("Starting client")
    val client = Await.result(clientSettings.createClient(), Duration.Inf)

    val discordAPI = new DiscordAPI(discordConfig.whitelistedServersConfig, client)
    val sharedState = new SharedState
    val eventHandler = new DiscordEventHandler(discordAPI, sharedState)
    client.onEventSideEffects { c =>
      eventHandler.handler()(c)
    }
    client.login()
  }
}
