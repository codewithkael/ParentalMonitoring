package com.codewithkael.parentalmonitoring.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.codewithkael.parentalmonitoring.R
import com.codewithkael.parentalmonitoring.databinding.ActivityMainBinding
import com.codewithkael.parentalmonitoring.service.RTCService
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var views: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init(){
        views.apply {
            childLoginBtn.setOnClickListener {
                PermissionX.init(this@MainActivity).permissions(
                    android.Manifest.permission.RECORD_AUDIO,
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ).request{ allGranted,_,_ ->
                    if (allGranted){
                        startActivity(Intent(
                            this@MainActivity,ChildActivity::class.java
                        ).apply {
                            putExtra("username",childUsernameEt.text.toString().trim())
                            putExtra("password",childPasswordEt.text.toString().trim())
                        })
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "required permission are needed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }

            parentLoginBtn.setOnClickListener {
                startActivity(Intent(this@MainActivity,ParentActivity::class.java))
            }
            if (RTCService.isServiceRunning){
                finishAffinity()
            }
        }
    }
}