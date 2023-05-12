package com.codewithkael.parentalmonitoring.ui

import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.codewithkael.parentalmonitoring.MainRepository
import com.codewithkael.parentalmonitoring.databinding.ActivityParentBinding
import com.codewithkael.parentalmonitoring.utils.DataModel
import com.codewithkael.parentalmonitoring.utils.DataModelType.*
import com.codewithkael.parentalmonitoring.utils.LocationModel
import com.codewithkael.parentalmonitoring.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.webrtc.MediaStream
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ParentActivity : AppCompatActivity(), MainRepository.Listener, MainRepository.MediaStream,
    MainRepository.LocationListener {

    private lateinit var views: ActivityParentBinding

    @Inject
    lateinit var mainRepository: MainRepository
    private val randomString = Utils.randomString()

    private var prevMarker:Marker?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getInstance().load(this,PreferenceManager.getDefaultSharedPreferences(this))
        views = ActivityParentBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init() {
        mainRepository.init(randomString)
        mainRepository.listener = this
        mainRepository.streamListener = this
        mainRepository.locationListener = this
        views.apply {
            mainRepository.initSurfaceView(remoteView)
            observeBtn.setOnClickListener {
                if (childPasswordEt.text.toString().trim().isNotEmpty()
                    && childPasswordEt.text.toString().trim().isNotEmpty()
                ) {
                    mainRepository.sendMessageToSocket(
                        DataModel(
                            type = StartWatching,
                            username = randomString,
                            target = childUsernameEt.text.toString().trim(),
                            data = childPasswordEt.text.toString()
                        )
                    )
                } else {
                    Toast.makeText(
                        this@ParentActivity,
                        "fill the required fields",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            switchCamera.setOnClickListener {
                mainRepository.sendSwitchCameraCommand()
            }
            startLocating.setOnClickListener {
                mainRepository.sendMessageToSocket(
                    DataModel(
                        type = StartLocating,
                        username = randomString,
                        target = childUsernameEt.text.toString().trim(),
                        data = childPasswordEt.text.toString()
                    )
                )
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        mainRepository.onDestroy()
    }

    override fun onNewMessage(data: DataModel) {
        runOnUiThread {
            when (data.type) {
                UserExists -> {
                    Toast.makeText(
                        this@ParentActivity,
                        "couldn't log in,this user already exist.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

                FailedToFindUser -> {
                    Toast.makeText(this@ParentActivity, "could find child username", Toast.LENGTH_SHORT).show()}
                UserFoundSuccessfully -> {}
                EndCall -> {finish()}
                WrongPassword -> {
                    Toast.makeText(this@ParentActivity, "wrong password", Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }

    }

    override fun onStreamAdded(p0: MediaStream?) {
        p0?.videoTracks?.get(0)?.addSink(views.remoteView)
        runOnUiThread {
            views.apply {
                childUsernameEt.isEnabled =false
                childPasswordEt.isEnabled =false
                observeBtn.isEnabled =false
                switchCamera.isEnabled = true
            }
        }
    }

    override fun onNewLocation(location: LocationModel) {
        Timber.d("kael location receiving $location")
        if (!views.map.isVisible){
            views.map.isVisible = true
        }
        addNewMarker(location)
    }

    private fun addNewMarker(location: LocationModel) {
        prevMarker?.remove(views.map)
        views.map.controller.setZoom(18.0)
        val marker = Marker(views.map)
        val startPoint = GeoPoint(location.lat,location.lon)
        views.map.controller.setCenter(startPoint)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM)
        views.map.overlays.add(marker)
        prevMarker = marker

    }
}