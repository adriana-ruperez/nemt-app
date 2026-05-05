package com.mobapps.nemt

import android.app.Application
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.places.api.Places

class NemtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val key = BuildConfig.MAPS_API_KEY
        if (key.isNotBlank()) {
            MapsInitializer.initialize(applicationContext)
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, key)
            }
        }
    }
}
