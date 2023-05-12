package com.codewithkael.parentalmonitoring.webrtc

data class MyIceCandidate(
    val sdpMLineIndex: Double, val sdpMid: String, val sdpCandidate: String
)
