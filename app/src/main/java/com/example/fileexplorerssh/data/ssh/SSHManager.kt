package com.example.fileexplorerssh.data.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.util.Properties

class SSHManager {
    private var session: Session? = null
    private val tag = "SSH_CLI"

    fun connect(host: String, user: String, pass: String, port: Int): Boolean {
        Log.d(tag, "Attempting connection to $host:$port as $user")
        return try {
            val jsch = JSch()
            session = jsch.getSession(user, host, port)
            session?.setPassword(pass)
            val config = Properties().apply { put("StrictHostKeyChecking", "no") }
            session?.setConfig(config)
            session?.connect(10000)

            val connected = session?.isConnected == true
            Log.d(tag, "Connection result: $connected")
            connected
        } catch (e: Exception) {
            Log.e(tag, "Connect Error: ${e.message}")
            false
        }
    }

    fun executeCommand(command: String): String {
        Log.d(tag, "Executing command: $command")
        val currentSession = session ?: return "Error: No Session".also {
            Log.e(tag, "Command failed: No active session")
        }

        return try {
            val channel = currentSession.openChannel("exec") as ChannelExec
            channel.setCommand(command)
            val inputStream = channel.inputStream
            channel.connect()

            val output = StringBuilder()
            val reader = inputStream.bufferedReader()
            val buffer = CharArray(1024)
            var bytesRead: Int

            while (true) {
                while (reader.ready()) {
                    bytesRead = reader.read(buffer)
                    if (bytesRead <= 0) break
                    output.append(buffer, 0, bytesRead)
                }
                if (channel.isClosed) {
                    if (!reader.ready()) break
                }
                Thread.sleep(50)
            }
            channel.disconnect()
            val result = output.toString().trim()
            Log.d(tag, "Command result size: ${result.length} characters")
            result
        } catch (e: Exception) {
            Log.e(tag, "Execution Error: ${e.message}")
            "Error: ${e.message}"
        }
    }

    fun resolvePath(basePath: String, target: String): String {
        // Try to enter the target and get the absolute path
        val cmd = "cd \"$basePath\" && cd \"$target\" && pwd"
        val result = executeCommand(cmd)

        // Return the new path if successful, otherwise return the old one
        return if (result.startsWith("/") && !result.contains("Not a directory", ignoreCase = true)) {
            result
        } else {
            basePath
        }
    }

    fun getFileList(path: String): List<String> {
        Log.d(tag, "Fetching file list for: $path")
        val raw = executeCommand("ls -1LF \"$path\"")
        if (raw.isBlank() || raw.startsWith("Error")) {
            Log.w(tag, "No files found or error occurred at $path")
            return emptyList()
        }
        val list = raw.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        Log.d(tag, "Parsed ${list.size} items")
        return list
    }

    fun disconnect() {
        Log.d(tag, "Disconnecting session...")
        session?.disconnect()
        session = null
    }
}