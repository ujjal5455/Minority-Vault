package com.example.minorityvault.ai

import com.example.minorityvault.engine.QuestionId

/**
 * The yes/no questions. The engine only ever sees the stable [QuestionId]s; the text lives in string
 * resources so it can be translated. Never reuse or rename an id: saved matches refer to them.
 */
object QuestionBank {
    private val textById: Map<QuestionId, Int> = linkedMapOf(
        QuestionId("hot_dog") to R.string.question_hot_dog,
        QuestionId("pineapple_pizza") to R.string.question_pineapple_pizza,
        QuestionId("cereal_soup") to R.string.question_cereal_soup,
        QuestionId("morning_person") to R.string.question_morning_person,
        QuestionId("tea_coffee") to R.string.question_tea_coffee,
        QuestionId("aliens") to R.string.question_aliens,
        QuestionId("time_travel") to R.string.question_time_travel,
        QuestionId("cats_dogs") to R.string.question_cats_dogs,
        QuestionId("water_wet") to R.string.question_water_wet,
        QuestionId("ketchup_fridge") to R.string.question_ketchup_fridge,
        QuestionId("invisible_fly") to R.string.question_invisible_fly,
        QuestionId("remake") to R.string.question_remake,
        QuestionId("socks_sandals") to R.string.question_socks_sandals,
        QuestionId("reply_all") to R.string.question_reply_all,
        QuestionId("cricket_football") to R.string.question_cricket_football,
        QuestionId("spoilers") to R.string.question_spoilers,
        QuestionId("horror") to R.string.question_horror,
        QuestionId("robots") to R.string.question_robots,
        QuestionId("second_chai") to R.string.question_second_chai,
        QuestionId("lie_to_win") to R.string.question_lie_to_win,
        QuestionId("trust_left") to R.string.question_trust_left,
        QuestionId("money_happiness") to R.string.question_money_happiness,
        QuestionId("ghost") to R.string.question_ghost,
        QuestionId("socks_bed") to R.string.question_socks_bed,
        QuestionId("biryani") to R.string.question_biryani,
        QuestionId("phones_dinner") to R.string.question_phones_dinner,
        QuestionId("mountains_beach") to R.string.question_mountains_beach,
        QuestionId("early_late") to R.string.question_early_late,
        QuestionId("moon_year") to R.string.question_moon_year,
        QuestionId("superpower_secret") to R.string.question_superpower_secret,
        QuestionId("break_promise") to R.string.question_break_promise,
        QuestionId("pet_names") to R.string.question_pet_names,
    )

    val ids: List<QuestionId> = textById.keys.toList()

    /** The string resource for [id], or a placeholder if a newer host sent an id this build doesn't know. */
    fun textRes(id: QuestionId): Int = textById[id] ?: R.string.question_missing
}
