package com.lifeway.cqrsdemo.views

import com.lifeway.cqrsdemo.aggregate.NameChanged
import com.lifeway.cqrsdemo.aggregate.PersonCreated
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.GenericEventMessage
import org.axonframework.messaging.Message
import org.axonframework.messaging.interceptors.MessageHandlerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

class PersonView(val id: String, val firstName: String?, val lastName: String?)

@Repository
interface PersonViewRepo: MongoRepository<PersonView, String>

@ProcessingGroup("ProfileView")
@Component
class PersonViewHandler(private val personViewRepo: PersonViewRepo) {

    companion object {
        private val log = LoggerFactory.getLogger(PersonViewHandler::class.java)
    }

    @MessageHandlerInterceptor
    fun intercept(message: Message<GenericEventMessage<Any>>) {
        log.debug("Handling event for PersonViewHandler - ${message.payloadType}")
    }

    @EventHandler
    protected fun on(event: PersonCreated) {
        personViewRepo.save(event.let {
            PersonView(it.id, it.firstName, it.lastName)
        })
    }

    @EventHandler
    protected fun on(event: NameChanged) {
        personViewRepo.findById(event.personId).ifPresent {
            personViewRepo.save(PersonView(
                it.id,
                event.firstName ?: it.firstName,
                event.lastName ?: it.lastName
            ))
        }
    }
}
