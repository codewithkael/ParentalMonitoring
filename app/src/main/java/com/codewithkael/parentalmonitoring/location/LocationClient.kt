package com.codewithkael.parentalmonitoring.location

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationClient @Inject constructor(
    private val fusedLocationProviderClient: FusedLocationProviderClient
) {

    private var isLocationServiceIsRunning = false
    private var mLocationCallback:LocationCallback?=null
    var listener : Listener?=null

    @SuppressLint("MissingPermission")
    fun startSendingLocation() {
        if (!isLocationServiceIsRunning){
            isLocationServiceIsRunning = true
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    if (p0.locations.isNotEmpty()){
                        val currentLocation = p0.locations.last()
                        listener?.onNewLocation(currentLocation)
                    }
                }
            }

            val mLocationRequest = LocationRequest.create()
            mLocationRequest.interval = 5000
            mLocationRequest.fastestInterval = 5000
            mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fusedLocationProviderClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback!!,
                Looper.getMainLooper()
            )

        }
    }

    fun onDestroy(){
        mLocationCallback?.let {
            fusedLocationProviderClient.removeLocationUpdates(it)
        }
    }

    interface Listener{
        fun onNewLocation(loc: Location)
    }
}