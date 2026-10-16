package com.jash.salescashtracker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.jash.salescashtracker.data.Repository

class SalesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Same Firebase project as the web and Windows apps
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyAWFX3OrIyZYzrMyJEavsR3tA-FWvOI54k")
                .setApplicationId("1:860407957852:web:3dd332ad8c675172b1d999")
                .setProjectId("sales-cash-tracker")
                .setStorageBucket("sales-cash-tracker.firebasestorage.app")
                .setGcmSenderId("860407957852")
                .build()
            FirebaseApp.initializeApp(this, options)
        }
        Repository.start()
    }
}
