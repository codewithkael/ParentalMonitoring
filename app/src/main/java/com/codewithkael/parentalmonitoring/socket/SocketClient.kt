package com.codewithkael.parentalmonitoring.socket

import com.codewithkael.parentalmonitoring.utils.DataModel
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import timber.log.Timber
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Exception

@Singleton
class SocketClient @Inject constructor(
    private val gson:Gson
) {

    companion object {
        private var webSocket:WebSocketClient?=null
    }

    private var username:String?=null
    var listener : Listener?=null

    fun init(username:String){
        this.username = username

        webSocket = object : WebSocketClient(URI("ws://10.0.2.2:3000")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                listener?.onSocketOpened()
                Timber.d("our client is connected now")
            }

            override fun onMessage(message: String?) {
                val dataModel = try {
                    gson.fromJson(message,DataModel::class.java)
                }catch (e:Exception){
                    null
                }
                dataModel?.let {

                    listener?.onNewMessage(it)
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                listener?.onSocketClosed()
                CoroutineScope(Dispatchers.IO).launch {
                    Timber.d("disconnected from socket server")

                    delay(5000)
                    init(username)
                    Timber.d("trying to reinit socket")
                }
            }

            override fun onError(ex: Exception?) {
                listener?.onSocketClosed()

            }

        }
        webSocket?.connect()


    }
    fun sendMessageToSocket(message:Any){
        try{
            webSocket?.send(gson.toJson(message))
        }catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun onDestroy(){
        webSocket?.close()
    }

    interface Listener {
        fun onNewMessage(message: DataModel)
        fun onSocketClosed()
        fun onSocketOpened()
    }
}