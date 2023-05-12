package com.codewithkael.parentalmonitoring.service

import android.content.Context
import android.content.Intent
import android.os.Build
import javax.inject.Inject

class RTCServiceRepository @Inject constructor(
    private val context:Context
) {

    fun startIntent(username:String,password:String){
        val startIntent = Intent(context,RTCService::class.java)
        startIntent.action = "StartIntent"
        startIntent.putExtra("username",username)
        startIntent.putExtra("password",password)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent)
        }else{
            context.startService(startIntent)
        }
    }

    fun stopIntent(){

        val startIntent = Intent(context,RTCService::class.java)
        startIntent.action = "StopIntent"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(startIntent)
        }else{
            context.startService(startIntent)
        }
    }
}