package com.lifeway.bootiful.ddd.utils

sealed class Try<T> {

    companion object {
        operator fun <T> invoke(body: () -> T): Try<T> {
            return try {
                Success(body())
            } catch (e: Exception) {
                Failure(e)
            }
        }
    }

    abstract fun isSuccess(): Boolean

    abstract fun isFailure(): Boolean

    fun <U> map(f: (T) -> U): Try<U> {
        return when (this) {
            is Success -> Try {
                f(this.value)
            }
            is Failure -> this as Failure<U>
        }
    }

    fun <U> flatMap(f: (T) -> Try<U>): Try<U> {
        return when (this) {
            is Success -> f(this.value)
            is Failure -> this as Failure<U>
        }
    }

    abstract fun get(): T

    abstract fun getOrElse(default:  T): T

    abstract fun orElse(default: Try<T>): Try<T>

    abstract fun <U> fold(fa: (Throwable) -> U, fb: (T) -> U): U

}

data class Success<T>(val value: T) : Try<T>() {
    override fun isSuccess(): Boolean = true
    override fun isFailure(): Boolean = false
    override fun getOrElse(default: T): T = value
    override fun get() = value
    override fun orElse(default: Try<T>): Try<T> = this
    override fun <U> fold(fa: (Throwable) -> U, fb: (T) -> U): U = try {
        fb(value)
    } catch (e: Exception) {
        fa(e)
    }
}

data class Failure<T>(val e: Throwable) : Try<T>() {
    override fun isSuccess(): Boolean = false
    override fun isFailure(): Boolean = true
    override fun getOrElse(default: T): T = default
    override fun get(): T = throw e
    override fun orElse(default: Try<T>): Try<T> = default
    override fun <U> fold(fa: (Throwable) -> U, fb: (T) -> U): U = fa(e)
}
