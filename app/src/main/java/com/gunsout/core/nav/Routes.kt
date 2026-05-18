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

    const val INGREDIENTS = "ingredients"
    const val INGREDIENT_EDIT = "ingredients/{ingredientId}"

    const val MEAL_PLANS = "mealplans"
    const val MEAL_PLAN_EDIT = "mealplans/{planId}"
    const val MEAL_TEMPLATE_EDIT = "templates/{templateId}"

    fun session(id: Long) = "session/$id"
    fun program(id: Long) = "programs/$id"
    fun history(id: Long) = "history/$id"
    fun exercise(id: Long) = "library/$id"
    fun ingredient(id: Long) = "ingredients/$id"
    fun mealPlan(id: Long) = "mealplans/$id"
    fun mealTemplate(id: Long) = "templates/$id"
}
