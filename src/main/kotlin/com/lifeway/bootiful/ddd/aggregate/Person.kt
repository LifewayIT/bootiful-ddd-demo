package com.lifeway.bootiful.ddd.aggregate

import com.lifeway.bootiful.ddd.services.MessageResponse
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.commandhandling.GenericCommandMessage
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.*
import org.axonframework.modelling.command.AggregateLifecycle.apply
import org.axonframework.spring.stereotype.Aggregate
import org.slf4j.LoggerFactory
import java.util.*

enum class ValidationStatus { PENDING, VALID, INVALID }
data class Email(val emailAddress: String, val isUsername: Boolean = false)
data class Address(val addressId: String, val line1: String, val line2: String?, val validationStatus: ValidationStatus)

@Aggregate
class Person() {

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

    @CommandHandlerInterceptor
    fun intercept(message: GenericCommandMessage<*>) {
        log.debug("Handling command for Person Aggregate - ${message.commandName}")
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.ALWAYS)
    fun handle(cmd: CreatePerson): MessageResponse {
        apply(PersonCreated(cmd.id, cmd.firstName, cmd.lastName, cmd.phoneNumber))
        val res = mapOf("name" to "${cmd.firstName} ${cmd.lastName}", "id" to cmd.id)
        return MessageResponse(
            message = "Created person successfully.",
            result = mapOf(
                "id" to cmd.id,
                "name" to "${cmd.firstName} ${cmd.lastName}"
            )
        )
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
    fun handle(command: AddAddress): MessageResponse {
        val addressId = UUID.randomUUID().toString().toLowerCase()
        val address = Address(
                addressId,
                command.line1,
                command.line2,
                ValidationStatus.PENDING
        )
        val event = AddressAdded(command.personId, addressId, address)
        apply(event)
        return MessageResponse(result = address, message = "Address was successfully created!")
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
