package com.example.garuda.domain.model

/**
 * A generic wrapper for repository/use-case results that carries
 * either a success value or an error with a human-readable message.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : AppResult<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data

    fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(message: String, exception: Throwable? = null) = Error(message, exception)
    }
}
