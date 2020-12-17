package com.lifeway.bootiful.ddd.views

import com.lifeway.bootiful.ddd.aggregate.AddressValidated
import com.lifeway.bootiful.ddd.aggregate.NameChanged
import com.lifeway.bootiful.ddd.aggregate.PersonCreated
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.GenericEventMessage
import org.axonframework.messaging.Message
import org.axonframework.messaging.interceptors.MessageHandlerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

data class Address(val addressId: String, val line1: String, val line2: String? = null)
data class Person(val firstName: String?, val lastName: String?)
data class ProfileView(val id: String, val person: Person, val addresses: List<Address> = listOf())

@Repository
interface ProfileViewRepo: MongoRepository<ProfileView, String>

@ProcessingGroup("ProfileView")
@Component
class ProfileViewHandler(private val profileViewRepo: ProfileViewRepo) {

    companion object {
        private val log = LoggerFactory.getLogger(ProfileViewHandler::class.java)
    }

    @MessageHandlerInterceptor
    fun intercept(message: Message<GenericEventMessage<Any>>) {
        log.debug("Handling event for ProfileViewHandler - ${message.payloadType}")
    }

    @EventHandler
    protected fun on(event: PersonCreated) {
        profileViewRepo.save(event.let {
            ProfileView(it.id, Person(it.firstName, it.lastName))
        })
    }

    @EventHandler
    protected fun on(event: NameChanged) {
        profileViewRepo.findById(event.personId).ifPresent {
            val person = Person(
                    event.firstName ?: it.person.firstName,
                    event.lastName ?: it.person.lastName
            )
            profileViewRepo.save(it.copy(person = person))
        }
    }

    @EventHandler
    protected fun on(event: AddressValidated) {
        profileViewRepo.findById(event.personId).ifPresent {
            val addresses = listOf(
                *it.addresses.toTypedArray(),
                    Address(event.address.addressId, event.address.line1, event.address.line2)
            )
            profileViewRepo.save(it.copy(addresses = addresses))
        }
    }
}
