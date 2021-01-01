package com.lifeway.bootiful.ddd

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.lifeway.bootiful.ddd.aggregate.Person
import com.lifeway.bootiful.ddd.utils.KafkaMessageSource
import com.mongodb.client.MongoClient
import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.extensions.mongo.DefaultMongoTemplate
import org.axonframework.extensions.mongo.eventhandling.saga.repository.MongoSagaStore
import org.axonframework.extensions.mongo.eventsourcing.eventstore.MongoEventStorageEngine
import org.axonframework.messaging.SubscribableMessageSource
import org.axonframework.serialization.json.JacksonSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories


@SpringBootApplication
@EnableMongoRepositories
@EnableReactiveMongoRepositories
class Application {

	@Bean
	fun eventStore(storageEngine: EventStorageEngine?): EmbeddedEventStore? {
		return EmbeddedEventStore
			.builder()
			.storageEngine(storageEngine)
			.build()
	}

	@Bean
	fun storageEngine(client: MongoClient?): EventStorageEngine? {
		val mapper = ObjectMapper().registerModule(KotlinModule()).setSerializationInclusion(JsonInclude.Include.NON_NULL)
		val ser = JacksonSerializer.builder().objectMapper(mapper).build()
		return MongoEventStorageEngine.builder()
			.mongoTemplate(
				DefaultMongoTemplate.builder().mongoDatabase(client).build()
			)
			.eventSerializer(ser)
			.build()
	}

	@Bean
	fun mySagaStore(client: MongoClient?): MongoSagaStore {
		return MongoSagaStore.builder().mongoTemplate(
			DefaultMongoTemplate.builder().mongoDatabase(client).build()
		).build()
	}

	/**
	 * Use a kafka event topic to distribute saved domain events to downstream event processors. (i.e. view handlers and sagas).
 	 */
	@Autowired
	fun configure(config: EventProcessingConfigurer, kafkaMessageSource: KafkaMessageSource, eventStore: EmbeddedEventStore) {
		config.usingSubscribingEventProcessors()
		config.registerSubscribingEventProcessor("InternalEventAdapter") {
			eventStore
		}
		config.configureDefaultSubscribableMessageSource {
			kafkaMessageSource
		}
	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			runApplication<Application>(*args)
		}
	}
}

