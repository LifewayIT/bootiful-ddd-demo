package com.lifeway.bootiful.ddd.services

import com.lifeway.bootiful.ddd.utils.Json
import com.lifeway.bootiful.ddd.utils.Try
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Component

private const val BROADCAST_TOPIC = "person:commandResponse"

interface ResponseBroadcastService {
    fun broadcastResponse(response: MessageResponse)
}

@Configuration
class RedisConfiguration {

    @Bean
    fun container(factory: ReactiveRedisConnectionFactory): ReactiveRedisMessageListenerContainer? {
        val container = ReactiveRedisMessageListenerContainer(factory)
        container.receive(ChannelTopic(BROADCAST_TOPIC))
        return container
    }

}

@Component
class RedisResponseBroadcastService(
        private val reactiveRedisTemplate: ReactiveRedisTemplate<String, String>,
        private val reactiveRedisMessageListenerContainer: ReactiveRedisMessageListenerContainer,
        private val commandService: CommandService,
): ResponseBroadcastService {

    companion object {
        val log = LoggerFactory.getLogger(RedisResponseBroadcastService::class.java)
    }

    init {
        reactiveRedisMessageListenerContainer
            .receive(ChannelTopic(BROADCAST_TOPIC))
            .map {
                log.debug("Received broadcast message: ${it.message}")

                val response = Try { Json.deserialize(it.message, MessageResponse::class) }
                response.fold(
                    { e -> log.error("Error serializing response: ${e.message} ") },
                    { s -> commandService.commitResponse(s) }
                )
            }
            .subscribe()
    }

    override fun broadcastResponse(response: MessageResponse) {
        reactiveRedisTemplate.convertAndSend(BROADCAST_TOPIC, Json.serialize(response)).subscribe()
    }
}
