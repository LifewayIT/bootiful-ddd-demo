package com.lifeway.bootiful.ddd

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.mongodb.client.MongoClient
import org.axonframework.config.EventProcessingConfigurer
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore
import org.axonframework.eventsourcing.eventstore.EventStorageEngine
import org.axonframework.extensions.mongo.DefaultMongoTemplate
import org.axonframework.extensions.mongo.eventhandling.saga.repository.MongoSagaStore
import org.axonframework.extensions.mongo.eventsourcing.eventstore.MongoEventStorageEngine
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
		val mapper = ObjectMapper().registerModule(KotlinModule())
		val ser = JacksonSerializer.builder().objectMapper(mapper).build()
		return MongoEventStorageEngine.builder()
			.mongoTemplate(
				DefaultMongoTemplate.builder().mongoDatabase(client).build()
			).
			eventSerializer(ser)
			.build()
	}

	@Bean
	fun mySagaStore(client: MongoClient?): MongoSagaStore {
		return MongoSagaStore.builder().mongoTemplate(
			DefaultMongoTemplate.builder().mongoDatabase(client).build()
		).build()
	}

	/**
	 * TOKEN TRACKING PROCESSORS DO NOT SCALE WELL: https://docs.axoniq.io/reference-guide/v/4.0/configuring-infrastructure-components/event-processing/event-processors.
	 * Essentially all processors would be managed by a single node, not properly distributing the traffic.
 	 */
	@Autowired
	fun configure(config: EventProcessingConfigurer) {
		config.usingSubscribingEventProcessors()
	}

// NO NEED FOR TOKEN STORE WHEN USING SUBSCRIBING EVENT PROCESSORS. SEE ABOVE.
//	@Bean(name = ["axonTokenStore"])
//	fun axonTokenStore(client: MongoClient?): TokenStore? {
//		val tokenSerializer: Serializer = XStreamSerializer.builder().build()
//		return MongoTokenStore.builder()
//			.serializer(tokenSerializer)
//			.mongoTemplate(DefaultMongoTemplate.builder().mongoDatabase(client).build())
//			.build()
//	}

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			runApplication<Application>(*args)
		}
	}
}

