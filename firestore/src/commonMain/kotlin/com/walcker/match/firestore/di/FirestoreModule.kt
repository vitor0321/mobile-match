package com.walcker.match.firestore.di

import com.walcker.match.firestore.FirestoreClient
import com.walcker.match.firestore.createFirestoreClient
import org.koin.dsl.module

/**
 * Koin module that registers the Firestore client as a singleton.
 * Products can inject [FirestoreClient] to access Firestore.
 */
public val firestoreModule = module {
    single<FirestoreClient> { createFirestoreClient() }
}
