package com.lifeway.bootiful.ddd.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlin.reflect.KClass

object Json {
    private val mapper = ObjectMapper().registerModule(KotlinModule())
    fun serialize(any: Any): String = mapper.writeValueAsString(any)
    fun <T: Any> deserialize(json: String, klass: KClass<T>): T {
        return mapper.readValue(json, klass.java)
    }
    fun deserialize(json: String): JsonNode {
        return mapper.readTree(json)
    }
}



