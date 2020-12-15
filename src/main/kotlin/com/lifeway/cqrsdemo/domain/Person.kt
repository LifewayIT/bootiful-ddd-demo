package com.lifeway.cqrsdemo.domain

import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.util.*

enum class ValidationStatus { PENDING, VALID, INVALID }
data class Email(val emailAddress: String, val isUsername: Boolean = false)
data class Address(val addressId: String, val line1: String, val line2: String?, val validationStatus: ValidationStatus)

@Aggregate
class Person {

    companion object {
        private val log = LoggerFactory.getLogger(Person::class.java)
    }

    @AggregateIdentifier
    private var id: String? = null
    private var firstName: String? = null
    private var lastName: String? = null
    private var phoneNumber: String? = null
    private var emails: List<Email> = listOf()
    private var addresses: List<Address> = listOf()

    constructor()

    @CommandHandler
    constructor(cmd: CreatePerson) {
        apply(PersonCreated(cmd.id, cmd.firstName, cmd.lastName, cmd.phoneNumber))
    }

    @EventSourcingHandler
    fun on(event: PersonCreated) {
        this.id = event.id
        this.firstName = event.firstName
        this.lastName = event.lastName
        this.phoneNumber = event.phoneNumber
    }

    @CommandHandler
    fun handle(cmd: ChangeName) {
        apply(NameChanged(cmd.personId, cmd.firstName, cmd.lastName))
    }

    @EventSourcingHandler
    fun on(event: NameChanged) {
        val (_, firstName, lastName) = event
        this.lastName = lastName
        this.firstName = firstName
    }

    @CommandHandler
    fun handle(cmd: AddEmail) {
        if (emails.find { it.emailAddress == cmd.emailAddress } != null) {
            throw IllegalStateException("Email address already exists.")
        }
        apply(cmd.let { EmailAdded(it.personId, it.emailAddress, it.isUsername) })
    }

    @EventSourcingHandler
    fun on(event: EmailAdded) {
        emails = listOf(event.let { Email(it.emailAddress, it.isUsername) }, *emails.toTypedArray())
    }

    @CommandHandler
    fun handle(command: AddAddress) {
        val addressId = UUID.randomUUID().toString().toLowerCase()
        apply(
            command.let {
                AddressAdded(
                    it.personId,
                    addressId,
                    Address(
                        addressId,
                        it.line1,
                        it.line2,
                        ValidationStatus.PENDING
                    )
                )
            }
        )
    }

    @CommandHandler
    fun handle(command: ConfirmAddress) {
        apply(
            AddressValidated(
                command.personId,
                command.address.addressId,
                command.address
            )
        )
    }

    @CommandHandler
    fun handle(command: InvalidateAddress) {
        apply(
            AddressInvalidated(
                command.personId,
                command.address.addressId,
                command.address
            )
        )
    }

    @EventSourcingHandler
    fun on(event: AddressValidated) {
        addresses = listOf(
            event.address,
            *addresses.toTypedArray()
        )
    }
}
