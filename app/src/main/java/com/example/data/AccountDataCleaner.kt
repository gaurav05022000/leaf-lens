package com.example.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

object AccountDataCleaner {
    private const val TAG = "AccountDataCleaner"

    suspend fun deleteCloudData(
        db: FirebaseFirestore,
        storageRoot: StorageReference,
        uid: String
    ) {
        val userDoc = db.collection("users").document(uid)
        val subcollections = listOf("plants", "scan_history", "disease_logs", "usage", "daily_usage")
        for (collection in subcollections) {
            val snapshot = userDoc.collection(collection).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        }
        userDoc.delete().await()

        deleteStorageTree(storageRoot.child("users/$uid"))
    }

    private suspend fun deleteStorageTree(ref: StorageReference) {
        val listResult = try {
            ref.listAll().await()
        } catch (e: StorageException) {
            if (e.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) return
            throw e
        }

        for (item in listResult.items) {
            try {
                item.delete().await()
            } catch (e: StorageException) {
                if (e.errorCode != StorageException.ERROR_OBJECT_NOT_FOUND) {
                    Log.w(TAG, "Failed deleting storage item ${item.path}", e)
                    throw e
                }
            }
        }

        for (prefix in listResult.prefixes) {
            deleteStorageTree(prefix)
        }
    }
}
