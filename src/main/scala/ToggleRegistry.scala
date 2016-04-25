package com.osinka.play.toggles

trait ToggleRegistry {
  private val _toggles = scala.collection.mutable.Set.empty[Toggle]

  def toggles = _toggles.toSet

  def toggle(id: String)(strategyFactories: Strategy.Factory*): Toggle = {
    val t = new Toggle(id, strategyFactories :_*)
    _toggles += t
    t
  }
}
