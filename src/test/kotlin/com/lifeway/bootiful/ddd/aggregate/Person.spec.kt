package com.lifeway.bootiful.ddd.aggregate

import com.lifeway.bootiful.ddd.whenCommand
import org.axonframework.test.aggregate.AggregateTestFixture
import org.axonframework.test.aggregate.FixtureConfiguration
import java.util.*
import kotlin.test.Test

class PersonTests {
    private val fixture: FixtureConfiguration<Person> = AggregateTestFixture(Person::class.java)

    @Test
    fun testCreatePerson() {
        val personId = UUID.randomUUID().toString()

        fixture
            .givenNoPriorActivity()
            .whenCommand(CreatePerson(personId, "First", "Last"))
            .expectSuccessfulHandlerExecution()
            .expectEvents(
                PersonCreated(personId, "First", "Last")
            )
    }

}
