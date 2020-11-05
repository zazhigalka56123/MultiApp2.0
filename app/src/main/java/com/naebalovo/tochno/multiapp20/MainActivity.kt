package com.naebalovo.tochno.multiapp20

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import kotlinx.coroutines.*
import android.widget.ProgressBar
import bolts.AppLinks.getTargetUrlFromInboundIntent
import com.facebook.BuildConfig
import com.facebook.FacebookSdk
import com.facebook.applinks.AppLinkData
import com.facebook.applinks.AppLinks
import com.onesignal.OneSignal
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

private const val TAG = "MainA1ctivi"
private val backDeque: Deque<String> = LinkedList()

class MainActivity : AppCompatActivity() , FileChooseClient.ActivityChoser {

    private lateinit var webView: WebView
    private lateinit var preferences: SharedPreferences

    private var isConnected                                   = true
    private var alertDialog: AlertDialog?                     = null
    override var uploadMessage: ValueCallback<Array<Uri>>?    = null
    private var script : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        setContentView(R.layout.activity_main)
        script = intent.getStringExtra("script").toString()
        initWebView()
        startWebView(intent.getStringExtra("URL").toString())
    }

    private fun initWebView()                                                                  {

        toScroll(false)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val bit = Rect()
            window.decorView.getWindowVisibleDisplayFrame(bit)

            val osh = window.decorView.rootView.height
            val ka = osh - bit.bottom
            val kart = ka > osh * 0.1399
            toScroll(kart)
        }

        webView = findViewById(R.id.webView)
//    webView.visibility = WebView.GONE

        webView?.scrollBarStyle = WebView.SCROLLBARS_OUTSIDE_OVERLAY
        webView?.settings?.loadWithOverviewMode = true
        webView?.settings?.useWideViewPort = true
        webView?.settings?.javaScriptEnabled = true
        webView?.settings?.domStorageEnabled = true
        webView?.settings?.databaseEnabled = true
        webView?.settings?.setSupportZoom(false)
        webView?.settings?.allowFileAccess = true
        webView?.settings?.allowContentAccess = true
        webView?.settings?.loadWithOverviewMode = true
        webView?.settings?.useWideViewPort = true
        webView?.settings?.allowFileAccess = true
        webView?.settings?.domStorageEnabled = true
        webView?.settings?.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        webView?.settings?.mediaPlaybackRequiresUserGesture = true
    }
    private fun startWebView(startUrl:String)                                                  {
        webView?.loadUrl(startUrl)
        Log.d(TAG, "Start $startUrl")
        webView?.setNetworkAvailable(isConnected)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true)
        }

        webView?.webChromeClient = FileChooseClient(this)

        webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
//                        if (progressBar?.isShowing!!) {
//                            progressBar?.dismiss()
//                        }
                url?.let { if (it != "about:blank") addToDeque(it) }
                val queryId = preferences.getString("PREFS_QUERYID", "")
                webView?.evaluateJavascript(script) {
                    webView?.evaluateJavascript("q('$queryId');") {}
                    Log.d(TAG, "load")
                }
            }
            override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                alertDialog?.setTitle("Error")
                alertDialog?.setMessage(description)
                alertDialog?.show()
                if (errorCode == ERROR_TIMEOUT) {
                    view.stopLoading()
                }
            }
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                Uri.parse(url).getQueryParameter("cust_offer_id")
                    ?.let {
                        preferences.edit().putString("PREFS_QUERYID", it).apply()
                    }

                return false
            }
        }
        webView.visibility = WebView.VISIBLE

    }
    private fun toScroll(flag: Boolean)                                                        {
        if (flag){
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }else{
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean                             {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView!!.canGoBack()) {
            webView!!.goBack()
            return true
        }
        return false
    }
    override fun onBackPressed()                                                               {
        if (!goBackWithDeque()) {
            // Если в очереди нет ссылок, возвращаемся назад по умолчанию
            super.onBackPressed()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)            {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FileChooseClient.ActivityChoser.REQUEST_SELECT_FILE) {
            if (uploadMessage == null) return
            uploadMessage?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                )
            )
            uploadMessage = null
        }
    }


    //Memorizing five last urls-------------------------------------------------------------------------


    override fun onStop()                                                                      {
        super.onStop()
        preferences.edit().putString("PREFS_DEQUE", backDeque.reversed().joinToString(",")).apply()
    }
    private fun addToDeque(url: String)                                                        {
        if (backDeque.size > 5) {
            backDeque.removeLast()
        }
        backDeque.addFirst(url)
    }
    private fun goBackWithDeque(): Boolean                                                     {
        try {
            if (backDeque.size == 1) return false;


            backDeque.removeFirst()
            webView?.loadUrl(backDeque.first)


            backDeque.removeFirst()
            return true

        } catch (ex: NoSuchElementException) {
            ex.printStackTrace()
            return false
        }
    }

}