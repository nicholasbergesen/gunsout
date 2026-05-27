package com.gunsout.core.nav

object Routes {
    const val TODAY = "today"
    const val SESSION = "session/{sessionId}"
    const val DIET = "diet"
    const val BODY = "body"
    const val SETTINGS = "settings"

    const val PROGRAMS = "programs"
    const val PROGRAM_EDIT = "programs/{programId}"

    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/{sessionId}"

    const val LIBRARY = "library"
    const val LIBRARY_EDIT = "library/{exerciseId}"

    fun session(id: Long) = "session/$id"
    fun program(id: Long) = "programs/$id"
    fun history(id: Long) = "history/$id"
    fun exercise(id: Long) = "library/$id"
}
