package com.example.minorityvault.feature.round

import com.example.minorityvault.ai.QuestionBank
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.TwistId
import com.example.minorityvault.engine.TwistInfo

/** Sample data for previews only. */
internal object PreviewData {
    val asha = PlayerId("p1")
    val ben = PlayerId("p2")
    val chandrika = PlayerId("p3")
    val dev = PlayerId("p4")
    val ezra = PlayerId("p5")

    val players = listOf(
        PlayerUi(asha, "Asha", balance = 115),
        PlayerUi(ben, "Ben", balance = 62, brokenPromises = 2, hasLocked = true),
        PlayerUi(chandrika, "Chandrika", balance = 0, isDebtor = true),
        PlayerUi(dev, "Dev", balance = 90, hasLocked = true),
        PlayerUi(ezra, "Ezra", balance = 133),
    )

    val header = RoundHeaderUi(
        round = 3,
        totalRounds = 10,
        pot = 45,
        baseFine = 10,
        isAllIn = false,
        twist = TwistInfo(TwistId.EXACT_MINORITY, exactMinoritySize = 2),
        questionRes = QuestionBank.textRes(QuestionBank.ids.first()),
    )
}
