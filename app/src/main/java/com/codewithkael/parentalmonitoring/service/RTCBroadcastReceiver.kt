package com.codewithkael.parentalmonitoring.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RTCBroadcastReceiver : BroadcastReceiver() {
    @Inject lateinit var rtcServiceRepository: RTCServiceRepository
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action =="action_exit"){
            rtcServiceRepository.stopIntent()
        }
    }
}