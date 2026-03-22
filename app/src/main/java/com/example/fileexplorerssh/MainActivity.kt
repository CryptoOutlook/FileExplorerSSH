package com.example.fileexplorerssh

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.fileexplorerssh.data.ssh.SSHManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val sshManager = SSHManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("TEST", "App started")

        setContent {
            SSHScreen()
        }
    }

    @Composable
    fun SSHScreen() {

        var output by remember { mutableStateOf("Not connected") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),

            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // -------- CONNECT BUTTON --------
            Button(
                onClick = {
                    lifecycleScope.launch(Dispatchers.IO) {

                        val connected = sshManager.connect(
                            host = "192.168.0.127",
                            username = "u0_a586",
                            password = "Dhana@2211",
                            port = 42024
                        )

                        withContext(Dispatchers.Main) {
                            output = "Connected: $connected"
                            Log.d("SSH_TEST", output)
                        }
                    }
                }
            ) {
                Text("Connect SSH")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -------- RUN COMMAND --------
            Button(
                onClick = {
                    lifecycleScope.launch(Dispatchers.IO) {

                        Log.d("SSH_FLOW", "Running command")

                        val result = sshManager.executeCommand("ls")

                        withContext(Dispatchers.Main) {
                            output = result
                            Log.d("SSH_CMD", result)
                        }
                    }
                }
            ) {
                Text("Run LS Command")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Output:\n$output")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sshManager.disconnect()
    }
}
