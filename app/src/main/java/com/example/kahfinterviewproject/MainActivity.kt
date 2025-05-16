package com.example.kahfinterviewproject

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kahfinterviewproject.ui.theme.KahfInterviewProjectTheme
import org.xbill.DNS.ARecord
import org.xbill.DNS.CNAMERecord
import org.xbill.DNS.Lookup
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.Type
import java.net.URI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KahfInterviewProjectTheme {
                SimpleBrowserView()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    fun SimpleBrowserView() {
        var inputUrl by remember { mutableStateOf("https://www.google.com") }
        var webView by remember { mutableStateOf<WebView?>(null) }

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.Blue)
                        .padding(8.dp)
                        .padding(top = 20.dp)
                ) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = inputUrl,
                        onValueChange = { userGivenUrl ->
                            inputUrl = userGivenUrl
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val domain = extractDomain(inputUrl)
                                Thread {
                                    try {
                                        val privateDnsServer = "40.120.32.171"
                                        val resolver = SimpleResolver(privateDnsServer)
                                        val lookUp = Lookup(domain, Type.ANY)
                                        lookUp.setResolver(resolver)
                                        val result = lookUp.run()

                                        runOnUiThread {
                                            if (lookUp.result == Lookup.SUCCESSFUL && result != null && result.isNotEmpty()) {
                                                var isBlocked = false

                                                for (record in result) {
                                                    when (record) {
                                                        is ARecord -> {
                                                            val ip = record.address.hostAddress
                                                            if (ip.isNullOrBlank() || ip == "0.0.0.0") {
                                                                // Blank or invalid IP indicates blocked domain
                                                                isBlocked = true
                                                                break
                                                            }
                                                        }

                                                        is CNAMERecord -> {
                                                            val cname = record.target.toString()
                                                            if (cname.contains("blocked.kahfguard.com")) {
                                                                // blocked.kahfguard.com CName name indicates blocked domain
                                                                isBlocked = true
                                                                break
                                                            }
                                                        }
                                                    }
                                                }

                                                if (isBlocked) {
                                                    showBlockedView(this@MainActivity)
                                                } else {
                                                    loadUrlInWebView(webView, inputUrl)
                                                }
                                            } else {
                                                //Null result. So treat as blocked
                                                showBlockedView(this@MainActivity)
                                            }
                                        }

                                    } catch (e: Exception) {
                                        // No valid result. So treat as blocked
                                        runOnUiThread {
                                            showBlockedView(this@MainActivity)
                                        }
                                    }
                                }.start()
                            }
                        ),
                        label = {
                            Text(
                                modifier = Modifier.padding(vertical = 8.dp),
                                text = "Enter Url"
                            )
                        }

                    )
                }
            }
        ) { innerPadding ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        webViewClient = WebViewClient()

                        webView = this
                    }
                }
            )
        }
    }
}

private fun extractDomain(url: String): String {
    return try {
        URI(if (url.startsWith("http")) url else "http://$url").host ?: url
    } catch (e: Exception) {
        Log.e("BROWSER", "${e.message}")
        e.printStackTrace()
        url
    }
}

private fun loadUrlInWebView(webView: WebView?, filteredUrl: String) {
    webView?.loadUrl(filteredUrl)
}

private fun showBlockedView(mContext: Context) {
    Toast.makeText(mContext, "Website Blocked", Toast.LENGTH_SHORT).show()
}