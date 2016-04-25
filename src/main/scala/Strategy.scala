package com.osinka.play.toggles

import java.net.InetAddress
import java.util.Date
import scala.collection.convert.decorateAsScala._
import scala.util.control.Exception
import play.api.mvc.RequestHeader
import com.typesafe.config.ConfigException

trait Strategy {
  def enabled(request: RequestHeader): Option[Boolean]
}

object Strategy {
  type Factory = String => Strategy
}

class UserStrategies(userId: RequestHeader => Option[String]) {
  val whitelist: Strategy.Factory = (toggleId: String) => Whitelist(toggleId)
  def whitelist(strict: Boolean): Strategy.Factory = (toggleId: String) => Whitelist(toggleId, strict)
  val blacklist: Strategy.Factory = (toggleId: String) => Blacklist(toggleId)
  def gradual(percentage: Int): Strategy.Factory = (toggleId: String) => Gradual(toggleId, percentage)
  val anonymousOff: Strategy.Factory = (_: String) => AnonymousOff
  val anonymousOn: Strategy.Factory = (_: String) => AnonymousOn

  private def configUserlist(toggleId: String, listName: String) =
    Exception.catching(classOf[ConfigException.Missing]).opt {
      Set( Toggle.toggleConfig(toggleId).getStringList(listName).asScala :_* )
    } getOrElse Set.empty

  case class Whitelist(toggleId: String, strict: Boolean = false) extends Strategy {
    val userlist = configUserlist(toggleId, "whitelist")

    override def enabled(requestHeader: RequestHeader) =
      if (userId(requestHeader).exists(userlist.contains)) Some(true)
      else if (strict) Some(false)
      else None
  }

  case class Blacklist(toggleId: String) extends Strategy {
    val userlist = configUserlist(toggleId, "blacklist")

    override def enabled(requestHeader: RequestHeader) =
      if (userId(requestHeader).exists(userlist.contains)) Some(false)
      else None
  }

  case class Gradual(toggleId: String, percentage: Int) extends Strategy {
    import java.security.MessageDigest

    def md5(str: String) = {
      def algorithm = MessageDigest.getInstance("MD5")
      algorithm.digest(str.getBytes)
    }

    def hash(str: String) = md5(str)(0) & 0xFF

    override def enabled(requestHeader: RequestHeader) =
      userId(requestHeader) filter { userId =>
        hash(s"$toggleId:$userId") % 100 < percentage
      } map { _ =>
        true
      }
  }

  case object AnonymousOff extends Strategy {
    override def enabled(requestHeader: RequestHeader) =
      if (userId(requestHeader).isEmpty) Some(false)
      else None
  }

  case object AnonymousOn extends Strategy {
    override def enabled(requestHeader: RequestHeader) =
      if (userId(requestHeader).isEmpty) Some(true)
      else None
  }

}

object Strategies {
  def internalNet: Strategy.Factory = (_: String) => InternalNet()
  def internalNet(strict: Boolean): Strategy.Factory = (_: String) => InternalNet(strict)
  def releaseDate(when: Date): Strategy.Factory = (_: String) => ReleaseDate(when)

  case class InternalNet(strict: Boolean = false) extends Strategy {
    // Play 2.3.x may give us plain `X-Forwarded-For`
    def sanitizeIP(ip: String) = ip.split(""",\s*""").filterNot("unknown".==).last

    override def enabled(requestHeader: RequestHeader) = {
      val ip = sanitizeIP(requestHeader.remoteAddress)
      val addr = InetAddress.getByName(ip)

      if (addr.isAnyLocalAddress || addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress) Some(true)
      else if (strict) Some(false)
      else None
    }
  }

  case class ReleaseDate(when: Date) extends Strategy {
    override def enabled(requestHeader: RequestHeader) =
      if (when.after(new Date)) Some(true)
      else None
  }

  // TODO: Geo
}
