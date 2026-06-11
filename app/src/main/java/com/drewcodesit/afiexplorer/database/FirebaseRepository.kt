package com.drewcodesit.afiexplorer.database

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// This class is for future expansion to add trending search terms to the search UI
class FirebaseRepository {
    private val db = Firebase.firestore

    private val _trending = MutableStateFlow<List<String>>(emptyList())
    val trending: StateFlow<List<String>> = _trending

    // Adds search terms to Firebase Firestore or increments existing count
    fun trackSearch(query: String) {
        val docRef = db.collection("search_terms").document(query)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val newCount = (snapshot.getLong("count") ?: 0) + 1
            transaction.set(
                docRef,
                mapOf(
                    "count" to newCount,
                    "lastSearched" to System.currentTimeMillis()
                )
            )
        }.addOnSuccessListener {
            Log.d("FirebaseRepository", "Firestore write SUCCESS for query: $query")
        }.addOnFailureListener { e ->
            Log.e("FirebaseRepository", "Firestore write FAILED", e)
        }
    }

    // Loads the top 10 trending search terms from Firestore
    fun loadTrending() {
        db.collection("search_terms")
            .orderBy("count", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                _trending.value = result.map { it.id }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseRepository", "Failed to load trending terms", e)
            }
    }
}