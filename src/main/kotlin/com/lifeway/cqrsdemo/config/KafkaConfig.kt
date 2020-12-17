package com.lifeway.cqrsdemo.config

import com.lifeway.cqrsdemo.services.CommandWrapper
import com.lifeway.cqrsdemo.services.MessageResponse
import com.lifeway.cqrsdemo.services.ResponseBroadcastService
import com.lifeway.cqrsdemo.utils.Json
import com.lifeway.cqrsdemo.utils.Try
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.axonframework.commandhandling.callbacks.LoggingCallback
import org.axonframework.commandhandling.gateway.CommandGateway
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener
import org.springframework.kafka.support.KafkaHeaders.RECEIVED_MESSAGE_KEY
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.*


@Configuration
class KafkaConfig {

    @Autowired private val kafkaConsumer: KafkaConsumer? = null

    @Bean
    fun consumerConfigs(kafkaProperties: KafkaProperties): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers.joinToString()
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer().javaClass.name
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer().javaClass.name
        return props
    }

    @Bean
    fun kafkaListenerContainerFactory(kafkaProperties: KafkaProperties): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String?, String?>?>? {
        val factory: ConcurrentKafkaListenerContainerFactory<String?, String> = ConcurrentKafkaListenerContainerFactory()
        factory.consumerFactory = consumerFactory(kafkaProperties)
        factory.containerProperties.consumerRebalanceListener = kafkaConsumer
        return factory
    }

    @Bean
    fun consumerFactory(kafkaProperties: KafkaProperties): ConsumerFactory<String?, String?>? {
        val consumerFactory: DefaultKafkaConsumerFactory<String?, String?> = DefaultKafkaConsumerFactory(consumerConfigs(kafkaProperties))
        consumerFactory.setKeyDeserializer(StringDeserializer())
        consumerFactory.setValueDeserializer(StringDeserializer())
        return consumerFactory
    }
}

@Component
class KafkaConsumer(private val commandGateway: CommandGateway, private val broadcastService: ResponseBroadcastService): ConsumerAwareRebalanceListener {
    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }

    @KafkaListener(topics = ["\${kafka.commandTopic}"], groupId = "\${kafka.groupId}-commands")
    fun listenForCommands(message: String) {
        log.debug("Message consumed by command consumer: $message")
        val wrapper: CommandWrapper = Json.deserialize(message, CommandWrapper::class)
        val command = Json.deserialize(wrapper.payload, Class.forName("com.lifeway.cqrsdemo.domain.${wrapper.commandType}").kotlin)
        if (wrapper.awaitingReply) {
            val tryToSerialize = Try {
                val commandResult: Any? = commandGateway.sendAndWait(command)
                if (commandResult is MessageResponse)
                    commandResult.copy(commandId = wrapper.id)
                else
                    MessageResponse(commandId = wrapper.id)
            }
            val response = tryToSerialize.fold(
                { MessageResponse(wrapper.id, it.message, "An error occurred when dispatching your command.") },
                { it }
            )
            broadcastService.broadcastResponse(response)
        } else {
            commandGateway.send(command, LoggingCallback.INSTANCE)
        }
    }

    @KafkaListener(topics = ["\${kafka.eventTopic}"], groupId = "\${kafka.groupId}-event")
    fun listenForEvents(@Payload message: String?, @Header(RECEIVED_MESSAGE_KEY) key: String?) {
        log.debug(message, key)
    }
}

@Configuration
class KafkaProducerConfig {
    @Bean
    fun producerFactory(kafkaProperties: KafkaProperties): ProducerFactory<String, String> {
        val props: MutableMap<String, Any> = HashMap()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = kafkaProperties.bootstrapServers.joinToString()
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer().javaClass.name
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer().javaClass.name
        return DefaultKafkaProducerFactory(props)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory)
    }
}
