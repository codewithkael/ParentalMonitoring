package com.codewithkael.parentalmonitoring

import android.provider.ContactsContract.Data
import com.codewithkael.parentalmonitoring.location.LocationClient
import com.codewithkael.parentalmonitoring.socket.SocketClient
import com.codewithkael.parentalmonitoring.utils.DataModel
import com.codewithkael.parentalmonitoring.utils.DataModelType
import com.codewithkael.parentalmonitoring.utils.DataModelType.*
import com.codewithkael.parentalmonitoring.utils.DataModelType.IceCandidate
import com.codewithkael.parentalmonitoring.utils.LocationModel
import com.codewithkael.parentalmonitoring.webrtc.MyIceCandidate
import com.codewithkael.parentalmonitoring.webrtc.PeerConnectionObserver
import com.codewithkael.parentalmonitoring.webrtc.RTCClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.webrtc.*
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val socketClient: SocketClient,
    private val rtcClient: RTCClient,
    private val gson: Gson,
    private val locationClient: LocationClient
) : SocketClient.Listener, RTCClient.Listener, LocationClient.Listener {

    var listener:Listener?=null
    var locationListener:LocationListener?=null
    var streamListener:MediaStream?=null

    private lateinit var username: String
    private var password:String?=null
    private var localSurfaceView:SurfaceViewRenderer?=null
    private val sdpObserver = object : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {

        }

        override fun onSetSuccess() {
        }

        override fun onCreateFailure(p0: String?) {
        }

        override fun onSetFailure(p0: String?) {
        }
    }
    private var target:String=""

    fun init(
        username:String,password:String?=null,
        localSurfaceView:SurfaceViewRenderer?=null
    ){
        this.username = username
        this.password = password
        this.localSurfaceView = localSurfaceView
        initSocketClient(username)
        initRtcClient(username)
        locationClient.listener = this
    }

    private fun initSocketClient(username: String){
        socketClient.init(username)
        socketClient.listener = this
    }

    private fun initRtcClient(username: String){
        rtcClient.init(username,object : PeerConnectionObserver(){
            override fun onIceCandidate(p0: org.webrtc.IceCandidate?) {
                super.onIceCandidate(p0)
                p0?.let { rtcClient.sendIceCandidate(it,target) }
            }

            override fun onAddStream(p0: org.webrtc.MediaStream?) {
                super.onAddStream(p0)
                streamListener?.onStreamAdded(p0)
            }
        })
        rtcClient.listener = this
    }

    override fun onNewMessage(message: DataModel) {
        CoroutineScope(Dispatchers.Main).launch {
            listener?.onNewMessage(message)
            when(message.type){
                StartWatching -> handleStartWatching(message)
                StartLocating -> handleStartLocating(message)
                Offer -> handleOnOfferReceived(message)
                Answer -> handleOnAnswerReceived(message)
                IceCandidate -> handleOnIceCandidateReceived(message)
                SwitchCamera -> handleSwitchCamera(message)
                Location -> handleLocation(message)
                else -> {}
            }
        }

    }

    private fun handleLocation(message: DataModel) {
        val location = try {
            gson.fromJson(gson.toJson(message.data),LocationModel::class.java)
        } catch (e:Exception){
            null
        }
        location?.let {
            locationListener?.onNewLocation(it)
        }
    }

    private fun handleSwitchCamera(message: DataModel) {
        rtcClient.switchCamera()
    }

    private fun handleOnIceCandidateReceived(message: DataModel) {
        val candidate = try {
            gson.fromJson(gson.toJson(message.data),MyIceCandidate::class.java)
        }catch (e:Exception){
            null
        }

        candidate?.let {
            rtcClient.addIceCandidate(
                org.webrtc.IceCandidate(
                    it.sdpMid,it.sdpMLineIndex.toInt(),it.sdpCandidate
                )
            )
        }
    }

    private fun handleOnAnswerReceived(message: DataModel) {
        rtcClient.onRemoteSessionReceived(
            SessionDescription(
                SessionDescription.Type.ANSWER,
                message.data.toString()
            )
        )
    }

    private fun handleOnOfferReceived(message: DataModel) {
        rtcClient.onRemoteSessionReceived(
            SessionDescription(
                SessionDescription.Type.OFFER,message.data.toString()
            )
        )
        target = message.username
        rtcClient.answer(sdpObserver,target)
    }

    private fun handleStartLocating(message: DataModel) {
        target = message.username
        locationClient.startSendingLocation()
    }

    private fun handleStartWatching(message: DataModel) {
        target = message.username
        localSurfaceView?.let {
            rtcClient.initSurfaceView(it)
            rtcClient.startLocalVideoCapture(it)
        }
        rtcClient.call(sdpObserver,target)
    }

    override fun onSocketClosed() {
    }

    override fun onSocketOpened() {
        socketClient.sendMessageToSocket(
            DataModel(
                type = SignIn,
                username = username,
                data = password,
                target = null
            )
        )
    }

    fun restartRtcClient(){
        localSurfaceView?.release()
        rtcClient.closeCamera()
        rtcClient.closeConnection()
        initRtcClient(username)
    }

    fun sendSwitchCameraCommand(){
        socketClient.sendMessageToSocket(
            DataModel(
                type = SwitchCamera,
                username = username,
                target = target,
                data = null
            )
        )
    }
    fun onDestroy(){
        socketClient.sendMessageToSocket(DataModel(
            type = EndCall,
            username = username,
            target =target,
            data = null
        ))
        socketClient.onDestroy()
        rtcClient.closeCamera()
        rtcClient.closeCamera()
        locationClient.onDestroy()

    }

    fun initSurfaceView(remoteView: SurfaceViewRenderer) {
        rtcClient.initSurfaceView(remoteView)
    }

    fun sendMessageToSocket(data:DataModel) {
        socketClient.sendMessageToSocket(data)
    }

    interface Listener{
        fun onNewMessage(data:DataModel)
    }

    interface MediaStream{
        fun onStreamAdded(p0: org.webrtc.MediaStream?)
    }

    override fun onSendMessageToSocket(data: DataModel) {
        socketClient.sendMessageToSocket(data)
    }

    override fun onNewLocation(loc: android.location.Location) {
        socketClient.sendMessageToSocket(
            DataModel(type = Location,
            username = username, target = target,
            LocationModel(loc.latitude,loc.longitude)
            )
        )
    }

    interface LocationListener {
        fun onNewLocation(location: LocationModel)
    }
}