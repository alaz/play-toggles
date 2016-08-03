package com.osinka.play.toggles

import play.api.mvc.{Cookie, Cookies, Flash, RequestHeader}

object ToggleSet {
  val TOGGLES_TAG = "toggles"
  val TOGGLES_COOKIE = "toggles"

  def empty: Set[String] = Set.empty

  def current(implicit requestHeader: RequestHeader): Set[String] =
    from(requestHeader.tags) getOrElse empty

  def from(tags: Map[String, String]): Option[Set[String]] =
    tags.get(TOGGLES_TAG) map unserialize

  def from(cookies: Cookies): Option[Set[String]] =
    cookies.get(TOGGLES_COOKIE) map {_.value} map unserialize

  def tag(toggleSet: Set[String]) =
    TOGGLES_TAG -> serialize(toggleSet)

  def cookie(toggleSet: Set[String], maxAge: Option[Int] = None) =
    Cookie(TOGGLES_COOKIE, serialize(toggleSet), maxAge = maxAge)

  private def serialize(toggleSet: Set[String]): String = toggleSet.mkString("|")

  private def unserialize(s: String): Set[String] = s.split("""\|""").toSet
}