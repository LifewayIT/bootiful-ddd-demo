package com.lifeway.cqrsdemo.aggregate

data class PersonCreated(val id: String, val firstName: String, val lastName: String, val phoneNumber: String?)
data class NameChanged(val personId: String, val firstName: String?, val lastName: String?)
data class EmailAdded(val personId: String, val emailAddress: String, val isUsername: Boolean)
data class AddressAdded(
    val personId: String,
    val addressId: String,
    val address: Address,
)
data class AddressValidated(
    val personId: String,
    val addressId: String,
    val address: Address
)
data class AddressInvalidated(
    val personId: String,
    val addressId: String,
    val address: Address
)
