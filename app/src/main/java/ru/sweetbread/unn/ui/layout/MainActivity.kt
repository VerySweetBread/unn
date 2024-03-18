package ru.sweetbread.unn.ui.layout

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.sweetbread.unn.ui.Auditorium
import ru.sweetbread.unn.ui.Building
import ru.sweetbread.unn.ui.Discipline
import ru.sweetbread.unn.ui.KindOfWork
import ru.sweetbread.unn.ui.Lecturer
import ru.sweetbread.unn.ui.LecturerRank
import ru.sweetbread.unn.ui.ScheduleUnit
import ru.sweetbread.unn.ui.auth
import ru.sweetbread.unn.ui.getSchedule
import ru.sweetbread.unn.ui.theme.UNNTheme
import splitties.activities.start
import splitties.preferences.Preferences
import splitties.toast.toast
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import io.ktor.client.plugins.logging.*
import ru.sweetbread.unn.ui.Type
import ru.sweetbread.unn.ui.composes.Schedule
import ru.sweetbread.unn.ui.composes.ScheduleDay

object LoginData : Preferences("loginData") {
    var login by stringPref("login", "")
    var password by stringPref("password", "")
}

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


        if (LoginData.login.isEmpty() or LoginData.password.isEmpty()) start<LoginActivity>()
        runBlocking {
            if (!auth()) start<LoginActivity>()
        }

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