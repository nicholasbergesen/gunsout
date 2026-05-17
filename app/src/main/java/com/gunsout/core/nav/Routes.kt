package com.gunsout.core.nav

object Routes {
    const val TODAY = "today"
    const val SESSION = "session/{sessionId}"
    const val DIET = "diet"
    const val BODY = "body"
    const val SETTINGS = "settings"

    fun session(id: Long) = "session/$id"
}
