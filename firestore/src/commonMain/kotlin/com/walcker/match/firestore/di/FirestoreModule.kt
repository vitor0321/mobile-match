package com.walcker.match.firestore.di

import com.walcker.match.firestore.FirestoreClient
import com.walcker.match.firestore.createFirestoreClient
import org.koin.dsl.module

public val firestoreModule =
    module {
        single<FirestoreClient> { createFirestoreClient() }
    }
