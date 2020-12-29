package com.lifeway.bootiful.ddd

import org.axonframework.test.aggregate.ResultValidator
import org.axonframework.test.aggregate.TestExecutor
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

fun <T> TestExecutor<T>.whenCommand(command: Any): ResultValidator<T> = this.`when`(command)

