package com.example.fileexplorerssh

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.fileexplorerssh.data.ssh.SSHManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val sshManager = SSHManager()
    private val uiTag = "UI_ACTION"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(uiTag, "App onCreate: Initializing Edge-to-Edge")
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavigation()
                }
            }
        }
    }

    @Composable
    fun MainNavigation() {
        var currentScreen by remember { mutableStateOf("home") }
        var host by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var port by remember { mutableStateOf("22") }

        if (currentScreen == "home") {
            HomeScreen { h, u, p, prt ->
                Log.d(uiTag, "Navigating to GridScreen for host: $h")
                host = h; user = u; pass = p; port = prt
                currentScreen = "terminal"
            }
        } else {
            SSHGridScreen(host, user, pass, port.toIntOrNull() ?: 22) {
                Log.d(uiTag, "Home button clicked: Disconnecting and returning to login")
                sshManager.disconnect()
                currentScreen = "home"
            }
        }
    }

    @Composable
    fun HomeScreen(onConnect: (String, String, String, String) -> Unit) {
        var host by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }
        var port by remember { mutableStateOf("22") }
        val prefs = remember { getSharedPreferences("ssh_prefs", Context.MODE_PRIVATE) }
        var savedList by remember { mutableStateOf(prefs.getStringSet("saves", emptySet())?.toList() ?: emptyList()) }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SSH Login", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host IP") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next))
                    OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next))
                    OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done))
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            Log.d(uiTag, "Connect button clicked: $host")
                            onConnect(host, user, pass, port)
                        }, modifier = Modifier.weight(1f)) { Text("Connect", color = Color.White) }

                        Button(onClick = {
                            Log.d(uiTag, "Save button clicked for: $host")
                            val set = savedList.toMutableSet().apply { add("$host|$user|$pass|$port") }
                            prefs.edit { putStringSet("saves", set) }
                            savedList = set.toList()
                        }) { Text("Save", color = Color.White) }
                    }
                }
            }
            if (savedList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedList) { save ->
                        val p = save.split("|")
                        SuggestionChip(onClick = {
                            Log.d(uiTag, "Saved Chip clicked: ${p[0]}")
                            host = p[0]; user = p[1]; pass = p[2]; port = p[3]
                        }, label = { Text(p[0], color = Color.White) })
                    }
                }
            }
        }
    }

    @Composable
    fun SSHGridScreen(h: String, u: String, p: String, prt: Int, onHome: () -> Unit) {
        var currentPath by remember { mutableStateOf(".") }
        var fileList by remember { mutableStateOf(emptyList<String>()) }
        var terminalOutput by remember { mutableStateOf("Connected to ") }
        val scope = rememberCoroutineScope()

        val navigateTo: (String) -> Unit = { target ->
            Log.d(uiTag, "Navigation triggered. Target: $target, From Path: $currentPath")
            scope.launch(Dispatchers.IO) {
                val resolved = sshManager.resolvePath(currentPath, target)
                val list = sshManager.getFileList(resolved)
                withContext(Dispatchers.Main) {
                    currentPath = resolved
                    fileList = list
                }
            }
        }

        LaunchedEffect(Unit) {
            Log.d(uiTag, "SSHGridScreen launched. Initializing connection...")
            scope.launch(Dispatchers.IO) {
                if (sshManager.connect(h, u, p, prt)) {
                    val startPath = sshManager.executeCommand("pwd")
                    Log.d(uiTag, "Initial PWD: $startPath")
                    withContext(Dispatchers.Main) {
                        currentPath = startPath
                        terminalOutput = "Connected to $h ($u)"
                    }
                    navigateTo(startPath)

                } else {
                    withContext(Dispatchers.Main) { terminalOutput = "Connection Failed" }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            Text("Path: $currentPath", color = Color.Yellow, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))

            LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), modifier = Modifier.weight(1f)) {
                items(fileList) { raw ->
                    val isLink = raw.contains("->")
                    val isDir = raw.endsWith("/") || isLink
                    val cleanName = if (isLink) raw.substringBefore("->").trim().trimEnd('@')
                    else raw.trimEnd('/', '*', '@', '|', '=')

                    Button(
                        onClick = {
                            Log.d(uiTag, "Item clicked: $cleanName")
                            scope.launch(Dispatchers.IO) {
                                // 1. Try to resolve the path as if it's a directory
                                val resolved = sshManager.resolvePath(currentPath, cleanName)

                                withContext(Dispatchers.Main) {
                                    if (resolved != currentPath) {
                                        // SUCCESS: The path changed, so it was a folder!
                                        Log.d(uiTag, "Successfully entered folder: $cleanName")
                                        currentPath = resolved
                                        // Now refresh the file list for the new path
                                        scope.launch(Dispatchers.IO) {
                                            val list = sshManager.getFileList(resolved)
                                            withContext(Dispatchers.Main) { fileList = list }
                                        }
                                    } else {
                                        // FAILURE: Path didn't change, so it's a file.
                                        // Now we run the file info command.
                                        Log.d(uiTag, "Not a folder, fetching file info for: $cleanName")
                                        scope.launch(Dispatchers.IO) {
                                            val out = sshManager.executeCommand("ls -ld \"$currentPath/$cleanName\"")
                                            withContext(Dispatchers.Main) { terminalOutput = out }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("${if (isDir) "📁" else "📄"} $cleanName", color = Color.White, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    Log.d(uiTag, "Grid Navigation: Home clicked")
                    onHome()
                }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Home, null, tint = Color.White); Text("Home", color = Color.White) }

                Button(onClick = {
                    Log.d(uiTag, "Grid Navigation: Refresh clicked for path $currentPath")
                    navigateTo(currentPath)
                }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh, null, tint = Color.White); Text("Refresh", color = Color.White) }

                Button(onClick = {
                    Log.d(uiTag, "Grid Navigation: Back clicked from $currentPath")
                    navigateTo("..")
                }, modifier = Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White); Text("Back", color = Color.White) }
            }

            Card(colors = CardDefaults.cardColors(containerColor = Color.Black), modifier = Modifier.fillMaxWidth().height(100.dp)) {
                Text(terminalOutput, color = Color(0xFF00FF41), fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState()))
            }
        }
    }
}