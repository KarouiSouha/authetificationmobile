package com.health.virtualdoctor.ui.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class WebSocketManager {
    private var webSocket: WebSocket? = null
    private var listener: WebSocketEventListener? = null

    fun connectWebSocket(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                listener?.onWebSocketOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                listener?.onWebSocketMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                listener?.onWebSocketMessage(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                listener?.onWebSocketClosing(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener?.onWebSocketClosed(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                listener?.onWebSocketFailure(t)
            }
        })
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Closing connection")
        webSocket = null
    }

    fun setEventListener(listener: WebSocketEventListener) {
        this.listener = listener
    }

    interface WebSocketEventListener {
        fun onWebSocketOpen()
        fun onWebSocketMessage(message: String)
        fun onWebSocketClosing(code: Int, reason: String)
        fun onWebSocketClosed(code: Int, reason: String)
        fun onWebSocketFailure(t: Throwable)
    }
}