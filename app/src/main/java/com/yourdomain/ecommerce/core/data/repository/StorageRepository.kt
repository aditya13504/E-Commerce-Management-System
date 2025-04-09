package com.yourdomain.ecommerce.core.data.repository

import android.net.Uri
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Repository for handling file storage operations with Supabase Storage
 * With enhanced error handling for common Supabase issues
 */
class StorageRepository private constructor(
    private val supabaseClient: SupabaseClient
) {
    private val tag = "StorageRepository"
    
    // Add singleton implementation
    companion object {
        @Volatile
        private var INSTANCE: StorageRepository? = null
        
        fun getInstance(supabaseClient: SupabaseClient): StorageRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StorageRepository(supabaseClient).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Upload a file to Supabase Storage with enhanced error handling
     * @param bucketName Storage bucket name
     * @param filePath Path within the bucket where the file will be stored
     * @param file File to upload
     * @param contentType MIME type of the file (e.g., "image/jpeg")
     */
    suspend fun uploadFile(
        bucketName: String,
        filePath: String,
        file: File,
        contentType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create the bucket if it doesn't exist
            ensureBucketExists(bucketName)
            
            // Handle the upload
            val path = supabaseClient.storage.from(bucketName).upload(
                path = filePath,
                file = file,
                contentType = contentType,
                upsert = true
            )
            
            // Return the public URL for the file
            val publicUrl = supabaseClient.storage.from(bucketName).publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            handleStorageException(e, "uploading file", filePath)
        }
    }
    
    /**
     * Upload a file from InputStream to Supabase Storage
     * @param bucketName Storage bucket name
     * @param filePath Path within the bucket where the file will be stored
     * @param inputStream Source InputStream
     * @param contentType MIME type of the file (e.g., "image/jpeg")
     */
    suspend fun uploadFile(
        bucketName: String,
        filePath: String,
        inputStream: InputStream,
        contentType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create the bucket if it doesn't exist
            ensureBucketExists(bucketName)
            
            // Handle the upload
            val path = supabaseClient.storage.from(bucketName).uploadInputStream(
                path = filePath,
                data = inputStream,
                contentType = contentType,
                upsert = true
            )
            
            // Return the public URL for the file
            val publicUrl = supabaseClient.storage.from(bucketName).publicUrl(path)
            Result.success(publicUrl)
        } catch (e: Exception) {
            handleStorageException(e, "uploading file from stream", filePath)
        }
    }
    
    /**
     * Download a file from Supabase Storage
     * @param bucketName Storage bucket name
     * @param filePath Path within the bucket where the file is stored
     * @param destination Local file destination
     */
    suspend fun downloadFile(
        bucketName: String,
        filePath: String,
        destination: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.storage.from(bucketName).download(filePath, destination)
            Result.success(destination)
        } catch (e: Exception) {
            handleStorageException(e, "downloading file", filePath)
        }
    }
    
    /**
     * Delete a file from Supabase Storage
     * @param bucketName Storage bucket name
     * @param filePath Path within the bucket where the file is stored
     */
    suspend fun deleteFile(
        bucketName: String,
        filePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            supabaseClient.storage.from(bucketName).delete(filePath)
            Result.success(Unit)
        } catch (e: Exception) {
            handleStorageException(e, "deleting file", filePath)
        }
    }
    
    /**
     * Get a public URL for a file
     * @param bucketName Storage bucket name
     * @param filePath Path within the bucket where the file is stored
     */
    suspend fun getPublicUrl(
        bucketName: String,
        filePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = supabaseClient.storage.from(bucketName).publicUrl(filePath)
            Result.success(url)
        } catch (e: Exception) {
            handleStorageException(e, "getting public URL", filePath)
        }
    }
    
    /**
     * List all files in a bucket
     * @param bucketName Storage bucket name
     * @param path Optional path to list files within (defaults to root)
     */
    suspend fun listFiles(
        bucketName: String,
        path: String = ""
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val files = supabaseClient.storage.from(bucketName).list(path)
            Result.success(files.map { it.name })
        } catch (e: Exception) {
            handleStorageException(e, "listing files", path)
        }
    }
    
    /**
     * Make sure a bucket exists, creating it if it doesn't
     */
    private suspend fun ensureBucketExists(bucketName: String) {
        try {
            // Check if bucket exists
            val buckets = supabaseClient.storage.retrieveBuckets()
            val exists = buckets.any { it.name == bucketName }
            
            if (!exists) {
                // Create the bucket with public access
                supabaseClient.storage.createBucket(bucketName, public = true)
                Log.d(tag, "Created bucket: $bucketName")
            }
        } catch (e: Exception) {
            // If we can't create it, we'll just try to use it and let that error out if needed
            Log.w(tag, "Error ensuring bucket exists: ${e.message}")
        }
    }
    
    /**
     * Centralized error handling for storage operations
     */
    private fun handleStorageException(
        e: Exception,
        operation: String,
        path: String
    ): Result<Nothing> {
        Log.e(tag, "Error $operation at $path: ${e.message}", e)
        
        return when (e) {
            is BadRequestRestException -> {
                // Handle the common "missing authorization header" error
                if (e.message?.contains("headers must have required property 'authorization'") == true) {
                    Log.e(tag, "Authorization header error. This usually means GoTrue plugin is not properly installed.", e)
                    Result.failure(
                        StorageException(
                            "Authentication error. Please log in again.",
                            StorageErrorType.AUTH_HEADER_MISSING
                        )
                    )
                } else {
                    Result.failure(StorageException(e.message ?: "Bad request", StorageErrorType.BAD_REQUEST))
                }
            }
            is RestException -> {
                when (e.status) {
                    HttpStatusCode.Unauthorized.value -> {
                        Result.failure(StorageException("Authentication required", StorageErrorType.UNAUTHORIZED))
                    }
                    HttpStatusCode.Forbidden.value -> {
                        Result.failure(StorageException("Not allowed. Check your permissions.", StorageErrorType.FORBIDDEN))
                    }
                    HttpStatusCode.NotFound.value -> {
                        Result.failure(StorageException("File not found: $path", StorageErrorType.NOT_FOUND))
                    }
                    else -> {
                        Result.failure(StorageException(e.message ?: "Storage error", StorageErrorType.SERVER_ERROR))
                    }
                }
            }
            is ClientRequestException -> {
                Result.failure(StorageException("Network error: ${e.message}", StorageErrorType.NETWORK_ERROR))
            }
            else -> {
                Result.failure(StorageException(e.message ?: "Unknown error", StorageErrorType.UNKNOWN))
            }
        }
    }
}

/**
 * Custom exceptions for storage operations
 */
class StorageException(
    message: String,
    val errorType: StorageErrorType
) : Exception(message)

/**
 * Enum to categorize storage errors for better UI handling
 */
enum class StorageErrorType {
    AUTH_HEADER_MISSING,  // Missing authorization header error
    UNAUTHORIZED,         // 401 errors
    FORBIDDEN,            // 403 errors
    NOT_FOUND,            // 404 errors
    BAD_REQUEST,          // 400 errors
    SERVER_ERROR,         // 500 errors
    NETWORK_ERROR,        // Connection issues
    UNKNOWN               // Anything else
} 