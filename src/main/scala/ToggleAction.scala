package com.osinka.play.toggles

import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc._
import org.slf4j.LoggerFactory

case class ToggleAction(toggles: Set[Toggle], cookieTTL: Option[Int] = None) {
  private val logger = LoggerFactory.getLogger(getClass)

  def apply[A](action: Action[A]) = Action.async[A](action.parser) { request =>
    // Eagerly calculate all registered toggles activation and store in tags
    val fromCookies = ToggleSet.from(request.cookies)
    val activated = toggles filter {_.activated(request)} map {_.id}

    logger.debug(s"Feature toggles active [${activated.mkString(",")}] for $request")

    val newRequest = new WrappedRequest[A](request) {
      override def tags = request.tags + ToggleSet.tag(activated)
    }

    action(newRequest) map { result =>
      // Store in cookie for future use
      if (fromCookies.isEmpty) {
        logger.debug(s"Storing feature toggles ${activated.mkString(",")} into cookie on $request")
        result.withCookies(ToggleSet.cookie(activated, cookieTTL))
      } else
        result
    }
  }
}

object ToggleAction {
  def apply(toggles: ToggleRegistry) = new ToggleAction(toggles.toggles)
}
