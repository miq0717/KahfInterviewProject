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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.kahfinterviewproject.ui.theme.KahfInterviewProjectTheme
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
                        .padding(8.dp)
                        .padding(top = 20.dp)
                ) {
                    TextField(
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
                                Thread{
                                    try {
                                        val privateDnsServer = "40.120.32.171"
                                        val resolver = SimpleResolver(privateDnsServer)
                                        val lookUp = Lookup(domain, Type.A)
                                        lookUp.setResolver(resolver)
                                        val result = lookUp.run()
                                        runOnUiThread {
//                                            Log.e("LOOKUP RESULT", "$result")
                                            if (lookUp.result == Lookup.SUCCESSFUL && result != null) {
                                                loadUrlInWebView(webView,inputUrl)
                                            } else {
//                                                Log.e("LOOKUP ELSE", "BLOCKED")
                                                showBlockedView(this@MainActivity)
                                            }
                                        }
                                    } catch (e: Exception) {
//                                        Log.e("THREAD CATCH", "${e.message}")
                                        runOnUiThread {
                                            showBlockedView(this@MainActivity)
                                        }
                                    }
                                }.start()
                            }
                        ),
                        label = {
                            Text("Enter Url")
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