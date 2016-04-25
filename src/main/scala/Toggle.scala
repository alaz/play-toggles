package com.osinka.play.toggles

import play.api.mvc.RequestHeader
import com.typesafe.config.{ConfigException, ConfigFactory}
import org.slf4j.LoggerFactory

case class Toggle(id: String, strategyFactories: Strategy.Factory*) {
  private val logger = LoggerFactory.getLogger(getClass)
  lazy val config = Toggle.toggleConfig(id)
  val strategies: Seq[Strategy] = strategyFactories map { _(id) }

  def active(implicit requestHeader: RequestHeader): Boolean =
    ToggleSet.current contains id

  private[toggles] def activated(requestHeader: RequestHeader): Boolean = {
    lazy val inConfig =
      if (config.hasPath("enabled")) Some(config.getBoolean("enabled"))
      else None

    lazy val inCookies = ToggleSet.from(requestHeader.cookies)

    lazy val byStrategy =
      for {
        strategy <- strategies.toStream
        enabled <- strategy.enabled(requestHeader)
      } yield {
        logger.debug(s"$requestHeader : $strategy for toggle $id = $enabled")
        enabled
      }

    if (inConfig contains false) false
    else if (inCookies.isDefined) inCookies exists {_ contains id}
    else if (inConfig contains true) true
    else if (byStrategy.headOption contains true) true
    else false
  }
}

object Toggle {
  import scala.collection.convert.decorateAsJava._
  import scala.util.control.Exception

  private val logger = LoggerFactory.getLogger(getClass)
  private[toggles] lazy val config = ConfigFactory.load.getConfig("toggles")

  def toggleConfig(id: String) =
    Exception.handling(classOf[ConfigException.Missing], classOf[ConfigException.WrongType]) by {
      case e: ConfigException.Missing =>
        logger.info(s"Configuration for toggle $id is empty")
        ConfigFactory.empty()

      case e: ConfigException.WrongType =>
        logger.warn(s"Configuration for toggle $id has wrong type, treating as disabled")
        ConfigFactory.parseMap(Map[String,AnyRef]("enabled" -> java.lang.Boolean.FALSE).asJava)
    } apply {
      config.getConfig(id)
    }
}