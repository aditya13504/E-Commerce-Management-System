package com.yourdomain.ecommerce.core.data

/**
 * A generic class that holds a value or an error message.
 * Used as a return type for repository operations to handle success and error cases.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    
    /**
     * Returns the encapsulated data if this instance represents [Success] or null
     * if it is [Error].
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }
    
    /**
     * Returns the encapsulated data if this instance represents [Success] or throws the encapsulated [Throwable] exception
     * if it is [Error].
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
    }
    
    /**
     * Returns true if this instance represents a successful outcome.
     */
    fun isSuccess(): Boolean = this is Success
    
    /**
     * Returns true if this instance represents a failed outcome.
     */
    fun isError(): Boolean = this is Error
    
    /**
     * Maps the result value if this instance is [Success].
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception)
    }
    
    /**
     * Returns a new [Result] with the given success value if this instance is [Success]
     * or with the original exception if it is [Error].
     */
    fun <R> fold(
        onSuccess: (T) -> R,
        onError: (Throwable) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(exception)
    }
    
    companion object {
        /**
         * Creates a success result with the given value.
         */
        fun <T> success(data: T): Result<T> = Success(data)
        
        /**
         * Creates an error result with the given exception.
         */
        fun <T> error(exception: Throwable): Result<T> = Error(exception)
        
        /**
         * Runs the given function and wraps its result in a [Result].
         * If an exception is thrown during execution, it is caught and wrapped in an [Error] result.
         */
        inline fun <T> runCatching(block: () -> T): Result<T> = try {
            Success(block())
        } catch (e: Throwable) {
            Error(e)
        }
    }
} 