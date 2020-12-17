package com.lifeway.cqrsdemo.services

import com.lifeway.cqrsdemo.utils.Json
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.DomainEventMessage
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.GenericEventMessage
import org.axonframework.messaging.Message
import org.axonframework.messaging.interceptors.MessageHandlerInterceptor
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.CompletableFuture


data class EnterpriseEvent <T> (val id: String, val eventType: String, val aggregateId: String, val payload: T, val timestamp: Date = Date(), val version: Int = 1)

@ProcessingGroup("InternalEventAdapter")
@Component
class InternalEventAdapter(private val kafkaTemplate: KafkaTemplate<String, String>) {

    companion object {
        private val log = LoggerFactory.getLogger(InternalEventAdapter::class.java)
    }

    @MessageHandlerInterceptor
    fun intercept(message: Message<GenericEventMessage<Any>>) {
        log.debug("Handling event for Internal Event Adapter - ${message.payloadType}")
    }

    private fun publish(enterpriseEvent: EnterpriseEvent<*>): CompletableFuture<Void> {
         return kafkaTemplate.send("events", enterpriseEvent.aggregateId, Json.serialize(enterpriseEvent)).completable().thenRun {
             log.debug("Sent event to enterprise: ${enterpriseEvent.id}")
         }
    }

    @EventHandler
    protected fun on(event: DomainEventMessage<*>): CompletableFuture<Void> {
        val enterpriseEvent = event.let { EnterpriseEvent(it.identifier, it.payload::class.java.simpleName, it.aggregateIdentifier, it.payload ) }
        return publish(enterpriseEvent)
    }
}
