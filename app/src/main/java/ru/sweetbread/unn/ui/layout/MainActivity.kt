package ru.sweetbread.unn.ui.layout

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import ru.sweetbread.unn.ui.composes.Schedule
import ru.sweetbread.unn.ui.theme.UNNTheme
import splitties.toast.toast

val client = HttpClient {
    install(HttpCache)
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Log.i("Ktor", message)
            }
        }
        level = LogLevel.ALL
    }
    install(HttpTimeout) {
        socketTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
    }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 3)
        exponentialDelay()
        modifyRequest { request ->
            request.headers.append("x-retry-count", retryCount.toString())
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UNNTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    var route by remember { mutableStateOf("home") }

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    onClick = { toast("Not implemented") },
                                    icon = {
                                        Icon(
                                            Icons.Filled.Home,
                                            contentDescription = "Home"
                                        )
                                    },
                                    selected = route.startsWith("home")
                                )

                                NavigationBarItem(
                                    onClick = {
                                        navController.navigate("journal/schedule")
                                        route = "journal/schedule" },
                                    icon = {
                                        Icon(
                                            Icons.Filled.DateRange,
                                            contentDescription = "Schedule"
                                        )
                                    },
                                    selected = route.startsWith("journal/")
                                )

                                NavigationBarItem(
                                    onClick = { toast("Not implemented") },
                                    icon = {
                                        Icon(
                                            Icons.Filled.AccountBox,
                                            contentDescription = "Account"
                                        )
                                    },
                                    selected = false
                                )
                            }
                        }
                    ) {innerPadding ->
                        Box(Modifier.padding(innerPadding)) {
                            NavHost(navController, startDestination = "home/blogposts") {
                                composable("home/blogposts") {
                                    Text("Not implemented")
                                }
                                composable("journal/schedule") {
                                    Schedule()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}