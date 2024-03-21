package ru.sweetbread.unn.ui

import android.app.Application
import android.content.Context
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.sender.HttpSender
import ru.sweetbread.unn.BuildConfig

class UNNApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        initAcra {
            buildConfigClass = BuildConfig::class.java
            reportFormat = StringFormat.JSON
            httpSender {
                uri = BuildConfig.ACRA_URL
                basicAuthLogin = BuildConfig.ACRA_LOGIN
                basicAuthPassword = BuildConfig.ACRA_PASS
                httpMethod = HttpSender.Method.POST
            }
        }
    }
}