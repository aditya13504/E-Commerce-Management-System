package com.yourdomain.ecommerce.core.data.repository

import android.util.Log
import com.yourdomain.ecommerce.core.data.model.Payment
import com.yourdomain.ecommerce.core.domain.repository.PaymentRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.ReturnFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Implementation of PaymentRepository that uses Supabase as data source
 */
class PaymentRepositoryImpl constructor(
    private val supabaseClient: SupabaseClient
) : PaymentRepository {

    private val tableName = "payments"
    private val tag = "PaymentRepository"

    override fun getAllPayments(page: Int, pageSize: Int): Flow<List<Payment>> = flow {
        try {
            val offset = (page - 1) * pageSize
            val payments = supabaseClient
                .from(tableName)
                .select(columns = Columns.raw("*")) {
                    Order.desc("payment_date") // Most recent payments first
                    limit(pageSize)
                    offset(offset)
                }
                .decodeList<Payment>()
            emit(payments)
        } catch (e: Exception) {
            Log.e(tag, "Error getting payments: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun getPaymentById(paymentId: String): Payment? {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("payment_id", paymentId)
                    }
                }
                .decodeSingleOrNull<Payment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting payment with ID $paymentId: ${e.message}", e)
            null
        }
    }

    override suspend fun getPaymentsByOrderId(orderId: String): List<Payment> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("order_id", orderId)
                    }
                    Order.desc("payment_date")
                }
                .decodeList<Payment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting payments for order $orderId: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun createPayment(payment: Payment): Result<Payment> {
        return try {
            // Ensure payment_date has a value if not provided
            val paymentToCreate = if (payment.paymentDate.isBlank()) {
                payment.copy(paymentDate = Instant.now().toString())
            } else {
                payment
            }
            
            val createdPayment = supabaseClient
                .from(tableName)
                .insert(paymentToCreate, returning = ReturnFormat.REPRESENTATION)
                .decodeSingle<Payment>()
                
            Result.success(createdPayment)
        } catch (e: Exception) {
            Log.e(tag, "Error creating payment: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updatePayment(paymentId: String, updates: Map<String, Any>): Result<Payment> {
        return try {
            // Build the JSON object for the update
            val updateData = buildJsonObject {
                updates.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        else -> {
                            // Convert other types to string if necessary
                            put(key, value.toString())
                        }
                    }
                }
                
                // Add updated_at timestamp
                put("updated_at", Instant.now().toString())
            }
            
            val updatedPayment = supabaseClient
                .from(tableName)
                .update(updateData, returning = ReturnFormat.REPRESENTATION) {
                    filter {
                        eq("payment_id", paymentId)
                    }
                }
                .decodeSingle<Payment>()
                
            Result.success(updatedPayment)
        } catch (e: Exception) {
            Log.e(tag, "Error updating payment with ID $paymentId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deletePayment(paymentId: String): Result<Boolean> {
        return try {
            supabaseClient
                .from(tableName)
                .delete {
                    filter {
                        eq("payment_id", paymentId)
                    }
                }
                
            Result.success(true)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting payment with ID $paymentId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getPaymentsByDateRange(startDate: LocalDate, endDate: LocalDate): List<Payment> {
        return try {
            // Convert LocalDate to ISO string for database comparison
            val startDateStr = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
            val endDateStr = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant().toString()
            
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        gte("payment_date", startDateStr)
                        lte("payment_date", endDateStr)
                    }
                    Order.desc("payment_date")
                }
                .decodeList<Payment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting payments between $startDate and $endDate: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getPaymentsByMethod(paymentMethod: String): List<Payment> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("payment_method", paymentMethod)
                    }
                    Order.desc("payment_date")
                }
                .decodeList<Payment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting payments with method $paymentMethod: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun getPaymentsByStatus(status: String): List<Payment> {
        return try {
            supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("payment_status", status)
                    }
                    Order.desc("payment_date")
                }
                .decodeList<Payment>()
        } catch (e: Exception) {
            Log.e(tag, "Error getting payments with status $status: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun processRefund(paymentId: String, amount: Double?, reason: String?): Result<Payment> {
        return try {
            // First get the payment to refund
            val payment = getPaymentById(paymentId)
            if (payment == null) {
                return Result.failure(Exception("Payment not found"))
            }
            
            // Create refund data
            val updates = mutableMapOf<String, Any>()
            updates["payment_status"] = "REFUNDED"
            
            if (reason != null) {
                updates["refund_reason"] = reason
            }
            
            // Update the payment with refund information
            updatePayment(paymentId, updates)
        } catch (e: Exception) {
            Log.e(tag, "Error processing refund for payment $paymentId: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun getTotalRevenueByPaymentMethod(): Map<String, Double> {
        return try {
            val payments = supabaseClient
                .from(tableName)
                .select {
                    filter {
                        eq("payment_status", "COMPLETED")
                    }
                }
                .decodeList<Payment>()
            
            // Group payments by method and sum the amounts
            payments.groupBy { it.paymentMethod }
                .mapValues { (_, payments) -> payments.sumOf { it.amountPaid } }
        } catch (e: Exception) {
            Log.e(tag, "Error calculating revenue by payment method: ${e.message}", e)
            emptyMap()
        }
    }

    override suspend fun countPayments(): Int {
        return try {
            val result = supabaseClient
                .from(tableName)
                .select(count = Count.exact, head = true)
                
            result.count?.toInt() ?: 0
        } catch (e: Exception) {
            Log.e(tag, "Error counting payments: ${e.message}", e)
            0
        }
    }
} 