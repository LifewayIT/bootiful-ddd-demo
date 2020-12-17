package com.lifeway.bootiful.ddd.aggregate

import org.axonframework.modelling.command.TargetAggregateIdentifier


data class CreatePerson(@TargetAggregateIdentifier val id: String, val firstName: String, val lastName: String, val phoneNumber: String? = null)
data class ChangeName(@TargetAggregateIdentifier val personId: String, val firstName: String, val lastName: String)
data class AddEmail(@TargetAggregateIdentifier val personId: String, val emailAddress: String, val isUsername: Boolean)
data class AddAddress(
    @TargetAggregateIdentifier val personId: String,
    val line1: String,
    val line2: String?
)

data class ConfirmAddress(
    @TargetAggregateIdentifier val personId: String,
    val address: Address
)
data class InvalidateAddress(
    @TargetAggregateIdentifier val personId: String,
    val address: Address
)
