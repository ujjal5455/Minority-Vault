package com.example.minorityvault.protocol

import com.example.minorityvault.engine.Contract
import com.example.minorityvault.engine.ContractId
import com.example.minorityvault.engine.ContractTerms
import com.example.minorityvault.engine.ContractTransfer
import com.example.minorityvault.engine.JudgedPromise
import com.example.minorityvault.engine.PlayerId
import com.example.minorityvault.engine.Promise
import com.example.minorityvault.engine.PromiseTarget

fun ContractTerms.toDto(): ContractTermsDto = when (this) {
    is ContractTerms.ShareWinnings -> ContractTermsDto.ShareWinnings(from.value, to.value, percent)
    is ContractTerms.FixedPayment -> ContractTermsDto.FixedPayment(from.value, to.value, amount, condition)
    is ContractTerms.VoteSale -> ContractTermsDto.VoteSale(debtor.value, buyer.value, price)
}

fun ContractTermsDto.toEngine(): ContractTerms = when (this) {
    is ContractTermsDto.ShareWinnings -> ContractTerms.ShareWinnings(PlayerId(from), PlayerId(to), percent)
    is ContractTermsDto.FixedPayment -> ContractTerms.FixedPayment(PlayerId(from), PlayerId(to), amount, condition)
    is ContractTermsDto.VoteSale -> ContractTerms.VoteSale(PlayerId(debtor), PlayerId(buyer), price)
}

fun Contract.toDto() = ContractDto(id.value, proposer.value, terms.toDto(), status)

fun ContractDto.toEngine() = Contract(ContractId(id), PlayerId(proposer), terms.toEngine(), status)

fun ContractTransfer.toDto() = ContractTransferDto(contractId.value, from.value, to.value, owed, paid)

fun ContractTransferDto.toEngine() = ContractTransfer(ContractId(contractId), PlayerId(from), PlayerId(to), owed, paid)

fun Promise.toDto() = PromiseDto(from.value, (to as? PromiseTarget.ToPlayer)?.id?.value, side)

fun PromiseDto.toEngine() =
    Promise(PlayerId(from), to?.let { PromiseTarget.ToPlayer(PlayerId(it)) } ?: PromiseTarget.Everyone, side)

fun JudgedPromise.toDto() = JudgedPromiseDto(promise.toDto(), verdict)

fun JudgedPromiseDto.toEngine() = JudgedPromise(promise.toEngine(), verdict)
