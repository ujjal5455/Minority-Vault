package com.example.minorityvault.ai

import com.example.minorityvault.engine.QuestionId

/**
 * The yes/no questions. The engine only ever sees the stable [QuestionId]s; the text lives in string
 * resources so it can be translated. Never reuse or rename an id: saved matches refer to them.
 */
object QuestionBank {
    private val textById: Map<QuestionId, Int> = linkedMapOf(
        QuestionId("table_liar") to R.string.question_table_liar,
        QuestionId("trust_left") to R.string.question_trust_left,
        QuestionId("lie_to_win") to R.string.question_lie_to_win,
        QuestionId("sell_friend") to R.string.question_sell_friend,
        QuestionId("know_betrayer") to R.string.question_know_betrayer,
        QuestionId("secret_ruin") to R.string.question_secret_ruin,
        QuestionId("pretend_like") to R.string.question_pretend_like,
        QuestionId("closer_than") to R.string.question_closer_than,
        QuestionId("replace_month") to R.string.question_replace_month,
        QuestionId("owe_apology") to R.string.question_owe_apology,
        QuestionId("read_messages") to R.string.question_read_messages,
        QuestionId("hear_thoughts") to R.string.question_hear_thoughts,
        QuestionId("rather_lose") to R.string.question_rather_lose,
        QuestionId("smartest_here") to R.string.question_smartest_here,
        QuestionId("friend_same_vote") to R.string.question_friend_same_vote,
        QuestionId("vote_as_said") to R.string.question_vote_as_said,
        QuestionId("honest_answer") to R.string.question_honest_answer,
        QuestionId("richest_deserve") to R.string.question_richest_deserve,
        QuestionId("poorest_fault") to R.string.question_poorest_fault,
        QuestionId("promise_worth") to R.string.question_promise_worth,
        QuestionId("smarter_majority") to R.string.question_smarter_majority,
        QuestionId("future_regret") to R.string.question_future_regret,
        QuestionId("break_promise") to R.string.question_break_promise,
        QuestionId("partner_phone") to R.string.question_partner_phone,
        QuestionId("tell_cheating") to R.string.question_tell_cheating,
        QuestionId("lost_wallet") to R.string.question_lost_wallet,
        QuestionId("dog_stranger") to R.string.question_dog_stranger,
        QuestionId("report_friend") to R.string.question_report_friend,
        QuestionId("salary_ask") to R.string.question_salary_ask,
        QuestionId("parents_marriage") to R.string.question_parents_marriage,
        QuestionId("late_disrespect") to R.string.question_late_disrespect,
        QuestionId("split_bill") to R.string.question_split_bill,
        QuestionId("ghosting_kinder") to R.string.question_ghosting_kinder,
        QuestionId("trust_after_cheat") to R.string.question_trust_after_cheat,
        QuestionId("favourite_child") to R.string.question_favourite_child,
        QuestionId("ex_friends") to R.string.question_ex_friends,
        QuestionId("cancel_last_minute") to R.string.question_cancel_last_minute,
        QuestionId("dream_job") to R.string.question_dream_job,
        QuestionId("good_unwatched") to R.string.question_good_unwatched,
        QuestionId("absolute_power") to R.string.question_absolute_power,
        QuestionId("forget_memory") to R.string.question_forget_memory,
        QuestionId("leak_secret") to R.string.question_leak_secret,
        QuestionId("cruel_famous") to R.string.question_cruel_famous,
        QuestionId("live_150") to R.string.question_live_150,
        QuestionId("simulation") to R.string.question_simulation,
        QuestionId("memory_wipe") to R.string.question_memory_wipe,
        QuestionId("live_again") to R.string.question_live_again,
        QuestionId("remembered") to R.string.question_remembered,
        QuestionId("real_you") to R.string.question_real_you,
        QuestionId("chose_today") to R.string.question_chose_today,
        QuestionId("younger_proud") to R.string.question_younger_proud,
        QuestionId("perfect_copy") to R.string.question_perfect_copy,
        QuestionId("painful_truth") to R.string.question_painful_truth,
        QuestionId("death_date") to R.string.question_death_date,
        QuestionId("one_life") to R.string.question_one_life,
        QuestionId("dreaming") to R.string.question_dreaming,
        QuestionId("wasting_life") to R.string.question_wasting_life,
        QuestionId("billion_years") to R.string.question_billion_years,
        QuestionId("unconditional_love") to R.string.question_unconditional_love,
        QuestionId("pineapple_pizza") to R.string.question_pineapple_pizza,
        QuestionId("biryani") to R.string.question_biryani,
    )

    val ids: List<QuestionId> = textById.keys.toList()

    /** The string resource for [id], or a placeholder if a newer host sent an id this build doesn't know. */
    fun textRes(id: QuestionId): Int = textById[id] ?: R.string.question_missing
}
