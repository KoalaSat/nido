package com.koalasat.nido.interfaces

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.fasterxml.jackson.core.JsonParseException
import com.koalasat.nido.Nido
import com.koalasat.nido.models.ExternalSigner
import com.vitorpamplona.quartz.events.Event
import org.json.JSONObject

class ExternalSignerInterface(private var webView: WebView) {
    @JavascriptInterface
    fun getPublicKey(): String {
        return Nido.getInstance().getHexKey()
    }

    @JavascriptInterface
    fun signEvent(
        event: String,
        callback: String,
    ) {
        Log.e("Nido", "Unsigned JSON $event")
        try {
            val eventObj = JSONObject(event)
            val jsonTags = eventObj.getJSONArray("tags")
            val tags: Array<Array<String>> =
                Array(jsonTags.length()) { index ->
                    val innerArray = jsonTags.getJSONArray(index)
                    Array(innerArray.length()) { innerIndex ->
                        innerArray.getString(innerIndex)
                    }
                }
            val unsignedEvent =
                Event(
                    id = eventObj.getString("id"),
                    pubKey = eventObj.getString("pubkey"),
                    createdAt = eventObj.getLong("created_at"),
                    kind = eventObj.getInt("kind"),
                    tags = tags,
                    content = eventObj.getString("content"),
                    sig = "",
                )
            Log.e("Nido", "Unsigned Event ${unsignedEvent.toJson()}")
            ExternalSigner.sign(unsignedEvent) { result ->
                val signedEvent =
                    Event(
                        id = unsignedEvent.id(),
                        pubKey = unsignedEvent.pubKey(),
                        createdAt = unsignedEvent.createdAt(),
                        kind = unsignedEvent.kind(),
                        tags = unsignedEvent.tags(),
                        content = unsignedEvent.content(),
                        sig = result,
                    )
                Log.e("Nido", "Signed Event ${signedEvent.toJson()}")
                sendResult(signedEvent.toJson(), callback)
            }
        } catch (e: JsonParseException) {
            Log.e("Nido", "Signed Event parse error: ${e.message}")
            sendResult("", callback)
        }
    }

    private fun sendResult(
        result: String,
        callback: String,
    ) {
        val jsCallback = "javascript:$callback($result)"
        webView.evaluateJavascript(jsCallback, null)
    }
}
