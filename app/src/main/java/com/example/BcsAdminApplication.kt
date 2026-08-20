package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class BcsAdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Throwable) {
            Log.d("BcsAdminApplication", "FirebaseApp initialization: ${e.message}")
        }
    }
}
