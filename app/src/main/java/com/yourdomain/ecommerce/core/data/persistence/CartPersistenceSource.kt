package com.yourdomain.ecommerce.core.data.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yourdomain.ecommerce.ui.viewmodels.CartItem // Adjust import if needed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

// Define DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cart_prefs")

class CartPersistenceSource(private val context: Context) {

    private val CART_KEY = stringPreferencesKey("cart_items_json")

    // Flow to observe cart changes from DataStore
    val cartFlow: Flow<Map<Int, CartItem>> = context.dataStore.data
        .map {
            val jsonString = it[CART_KEY]
            if (jsonString != null) {
                try {
                    Json.decodeFromString<Map<Int, CartItem>>(jsonString)
                } catch (e: Exception) {
                    println("Error decoding cart JSON: ${e.message}")
                    emptyMap<Int, CartItem>() // Return empty on error
                }
            } else {
                emptyMap<Int, CartItem>()
            }
        }

    // Save the entire cart map to DataStore
    suspend fun saveCart(cartItems: Map<Int, CartItem>) {
        try {
            val jsonString = Json.encodeToString(cartItems)
            context.dataStore.edit {
                it[CART_KEY] = jsonString
            }
        } catch (e: Exception) {
            println("Error encoding cart JSON: ${e.message}")
        }
    }

    // Clear cart from DataStore
    suspend fun clearCart() {
        context.dataStore.edit {
            it.remove(CART_KEY)
        }
    }
} 