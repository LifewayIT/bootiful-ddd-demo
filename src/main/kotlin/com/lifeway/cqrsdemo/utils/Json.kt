package com.lifeway.cqrsdemo.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.lifeway.cqrsdemo.domain.Address
import kotlin.reflect.KClass


object Json {
    private val mapper = ObjectMapper().registerModule(KotlinModule())
    fun serialize(any: Any): String = mapper.writeValueAsString(mapper)
    fun <T: Any> deserialize(json: String, klass: KClass<T>): T {
        return mapper.readValue(json, klass.java)
    }
    fun deserialize(json: String): JsonNode {
        return mapper.readTree(json)
    }
}



