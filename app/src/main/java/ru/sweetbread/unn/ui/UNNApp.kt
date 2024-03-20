package ru.sweetbread.unn.ui

import android.app.Application
import android.content.Context
import org.acra.BuildConfig
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender

class UNNApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        initAcra {
            //core configuration:
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            httpSender {
                uri = "/report"
                basicAuthLogin = "5Fh9roh02J2BUyNl"
                basicAuthPassword = "tTWalio7NVosHohT"
                httpMethod = HttpSender.Method.POST
            }
        }
    }
}