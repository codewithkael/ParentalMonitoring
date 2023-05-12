package com.codewithkael.parentalmonitoring.webrtc

import android.app.Application
import android.content.Context
import com.codewithkael.parentalmonitoring.utils.DataModel
import com.codewithkael.parentalmonitoring.utils.DataModelType
import org.webrtc.*
import org.webrtc.PeerConnection.Observer
import javax.inject.Inject

class RTCClient @Inject constructor(
    private val context: Application
) {

    var listener: Listener?=null

    private lateinit var username: String
    private lateinit var observer: Observer
    private var peerConnection:PeerConnection?=null
    private val rootEglBase = EglBase.create()
    private var localStream:MediaStream?=null
    private val localTrackId = "local_track"
    private val localStreamId = "local_stream"
    private var localAudioTrack:AudioTrack?=null
    private var localVideoTrack:VideoTrack?=null
    private var camera2Enumerator:Camera2Enumerator?=null
    private val videoCapturer = getVideoCapturer(context)

    private val peerConnectionFactory by lazy { buildPeerConnectionFactory() }

    private val iceServer = listOf(
        PeerConnection.IceServer(
            "turn:openrelay.metered.ca:443?transport=tcp", "openrelayproject", "openrelayproject"
        )
    )

    private var serviceViewRenderer:SurfaceViewRenderer?=null

    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }

    fun init(username: String, observer: Observer) {
        this.username = username
        this.observer = observer
        initPeerConnectionFactory(context)
        peerConnection = buildPeerConnection(observer)
    }

    private fun buildPeerConnection(observer: Observer): PeerConnection? {
       return peerConnectionFactory.createPeerConnection(
            iceServer,observer
        )
    }

    private fun buildPeerConnectionFactory() : PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory((DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(rootEglBase.eglBaseContext,true,true)
            )
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            }).createPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    fun initSurfaceView(view:SurfaceViewRenderer) = view.run {
       serviceViewRenderer = this
        setMirror(true)
        setEnableHardwareScaler(true)
        init(rootEglBase.eglBaseContext,null)
    }

    fun startLocalVideoCapture(localVideo:SurfaceViewRenderer){
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name,
        rootEglBase.eglBaseContext)

        videoCapturer.initialize(
            surfaceTextureHelper,localVideo.context,localVideoSource.capturerObserver
        )

        videoCapturer.startCapture(
            720,480,20
        )

        localAudioTrack = peerConnectionFactory.createAudioTrack(localTrackId+"_audio",localAudioSource)
        localVideoTrack = peerConnectionFactory.createVideoTrack(localTrackId+"_video",localVideoSource)
        localStream = peerConnectionFactory.createLocalMediaStream(localStreamId)
        localStream?.addTrack(localAudioTrack)
        localStream?.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)
    }

    private fun getVideoCapturer(context:Context) : CameraVideoCapturer =
        Camera2Enumerator(context).run {
            camera2Enumerator = this
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it,null)
            } ?: throw IllegalStateException()
        }

    private fun PeerConnection.call(sdpObserver: SdpObserver,target:String){
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        }
        createOffer(object : SdpObserver by sdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        listener?.onSendMessageToSocket(
                            DataModel(type = DataModelType.Offer,
                            username = username,
                            target = target,
                            data = desc?.description)
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                },desc)
            }
        },constraints)
    }

    private fun PeerConnection.answer(sdpObserver: SdpObserver,target: String){
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        }

        createAnswer(object : SdpObserver by sdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        listener?.onSendMessageToSocket(
                            DataModel(
                                type = DataModelType.Answer,
                                username = username,
                                target = target,
                                data = desc?.description
                            )
                        )
                    }

                    override fun onCreateFailure(p0: String?) {
                    }

                    override fun onSetFailure(p0: String?) {
                    }

                },desc)
            }
        },constraints)
    }

    fun call(sdpObserver: SdpObserver,target: String) = peerConnection?.call(sdpObserver,target)
    fun answer(sdpObserver: SdpObserver,target: String) = peerConnection?.answer(sdpObserver,target)

    fun onRemoteSessionReceived(sessionDescription: SessionDescription){
        peerConnection?.setRemoteDescription(object : SdpObserver{
            override fun onCreateSuccess(p0: SessionDescription?) {

            }

            override fun onSetSuccess() {
            }

            override fun onCreateFailure(p0: String?) {
            }

            override fun onSetFailure(p0: String?) {
            }

        },sessionDescription)
    }

    fun addIceCandidate(iceCandidate:IceCandidate){
        peerConnection?.addIceCandidate(iceCandidate)
    }

    fun sendIceCandidate(candidate: IceCandidate,target: String){
        addIceCandidate(candidate)
        val candidateConstant = hashMapOf(
            "sdpMid" to candidate.sdpMid,
            "sdpMLineIndex" to candidate.sdpMLineIndex,
            "sdpCandidate" to candidate.sdp
        )

        listener?.onSendMessageToSocket(
            DataModel(
                type = DataModelType.IceCandidate,
                username = username,
                target = target,
                data = candidateConstant
            )
        )
    }

    fun switchCamera(){
        videoCapturer.switchCamera(null)
    }

    fun closeCamera(){
        videoCapturer.stopCapture()
        videoCapturer.dispose()
    }

    fun closeConnection(){
        localStream?.dispose()
        peerConnection?.close()
    }

    interface Listener {
        fun onSendMessageToSocket(data:DataModel)
    }
}