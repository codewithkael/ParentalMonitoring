package com.codewithkael.parentalmonitoring.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.codewithkael.parentalmonitoring.databinding.ActivityChildBinding
import com.codewithkael.parentalmonitoring.service.RTCServiceRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChildActivity  : AppCompatActivity(){

    private lateinit var views:ActivityChildBinding
    @Inject lateinit var rtcServiceRepository: RTCServiceRepository

    private lateinit var username:String
    private lateinit var password:String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityChildBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init(){

        username = intent.getStringExtra("username").toString()
        password = intent.getStringExtra("password").toString()
        rtcServiceRepository.startIntent(username,password)
        finishAffinity()

    }
}