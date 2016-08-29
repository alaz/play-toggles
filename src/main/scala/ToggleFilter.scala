package com.osinka.play.toggles

import javax.inject.Inject
import scala.concurrent.Future
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import akka.stream.Materializer
import org.slf4j.LoggerFactory

class ToggleFilter @Inject() (implicit val mat: Materializer, configuration: Configuration, filterToggles: ToggleRegistry) extends Filter {
  private val logger = LoggerFactory.getLogger(getClass)

  val cookieTTL = configuration.getMilliseconds("toggles.cookieTTL") map { millis => (millis / 1000).toInt }
  val toggles = filterToggles.toggles

  def apply(nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    // Eagerly calculate all registered toggles activation and store in tags
    logger.debug(s"cookies is ${requestHeader.cookies}")
    val fromCookies = ToggleSet.from(requestHeader.cookies)
    logger.debug(s"ToggleSet is $fromCookies")
    logger.debug(s"Toggles is ${toggles}")
    val activated = toggles filter {_.activated(requestHeader)} map {_.id}
    val newRequestHeader = requestHeader.copy(tags = requestHeader.tags + ToggleSet.tag(activated))

    logger.debug(s"Feature toggles active [${activated.mkString(",")}] for $requestHeader")

    nextFilter(newRequestHeader) map { result =>
      // Store in cookie for future use
      if (!fromCookies.contains(activated)) {
        logger.debug(s"Storing feature toggles ${activated.mkString(",")} into cookie on $requestHeader")
        result.withCookies(ToggleSet.cookie(activated, cookieTTL))
      } else
        result
    }
  }
}
