package ru.sweetbread.unn.ui.layout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.sweetbread.unn.R
import ru.sweetbread.unn.ui.auth
import ru.sweetbread.unn.ui.theme.UNNTheme
import splitties.activities.start

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UNNTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val snackbarHostState = remember { SnackbarHostState() }
                    val scope = rememberCoroutineScope()

                    Scaffold(
                        snackbarHost = {
                            SnackbarHost(hostState = snackbarHostState)
                        }
                    ) { innerPadding ->
                        LoginPanel(Modifier.padding(innerPadding), { login, password ->
                            LoginData.login = login
                            LoginData.password = password
                            start<MainActivity>()
                        }, {
                            scope.launch {

                                snackbarHostState
                                    .showSnackbar(
                                        message = "Error",
                                        duration = SnackbarDuration.Long
                                    )
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun LoginPanel(
    modifier: Modifier = Modifier,
    ok: (login: String, password: String) -> Unit,
    error: () -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize(), Alignment.BottomCenter) {
        Column(
            modifier
                .padding(32.dp, 0.dp)
                .clip(RoundedCornerShape(10.dp, 10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(16.dp)
        ) {
            TextField(
                modifier = Modifier.padding(8.dp),
                value = login,
                onValueChange = { login = it },
                singleLine = true,
                label = { Text(stringResource(R.string.prompt_login)) }
            )

            TextField(
                modifier = Modifier.padding(8.dp),
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text(stringResource(R.string.prompt_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Button(modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), onClick = {
                loading = true
                scope.launch {
                    if (auth(login, password)) {
                        ok(login, password)
                    } else {
                        error()
                    }
                    loading = false
                }

            }) {
                Text(stringResource(R.string.sign_in))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginPreview() {
    UNNTheme {
        LoginPanel(ok = { _, _ -> }, error = {})
    }
}