package com.lifeway.bootiful.ddd.utils

import com.lifeway.bootiful.ddd.services.CommandWrapper
import com.lifeway.bootiful.ddd.services.EnterpriseEvent
import com.lifeway.bootiful.ddd.services.EnterpriseEventJson
import org.axonframework.common.Registration
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.EventMessage
import org.axonframework.eventhandling.GenericDomainEventMessage
import org.axonframework.messaging.SubscribableMessageSource
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener
import org.springframework.stereotype.Component
import java.util.concurrent.CopyOnWriteArraySet
import java.util.function.Consumer

@Component
class KafkaMessageSource: SubscribableMessageSource<EventMessage<*>>, ConsumerAwareRebalanceListener {

    private final val eventProcessors: MutableSet<Consumer<MutableList<out EventMessage<*>>>>  = CopyOnWriteArraySet()

    override fun subscribe(messageProcessor: Consumer<MutableList<out EventMessage<*>>>): Registration {
        this.eventProcessors.add(messageProcessor)

        return Registration {
            eventProcessors.remove(messageProcessor)
        }
    }

    @KafkaListener(topics = ["\${kafka.eventTopic}"], groupId = "\${kafka.groupId}-domain")
    fun listenForEvents(message: String) {
        val wrapper: EnterpriseEventJson = Json.deserialize(message, EnterpriseEventJson::class)
        val event = Json.deserialize(wrapper.payload, Class.forName("com.lifeway.bootiful.ddd.aggregate.${wrapper.eventType}").kotlin)
        val domainEventMessage: DomainEventMessage<*> = GenericDomainEventMessage("Person", wrapper.aggregateId, wrapper.sequence, event)
        eventProcessors.forEach {
            it.accept(mutableListOf(domainEventMessage))
        }
    }

}
