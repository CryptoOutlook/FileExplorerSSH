package com.example.fileexplorerssh.data.ssh

import android.util.Log
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Properties

class SSHManager {

    private var session: Session? = null

    // ---------------- CONNECT ----------------
    fun connect(
        host: String,
        username: String,
        password: String,
        port: Int = 22
    ): Boolean {

        return try {
            Log.d("SSH_TEST", "Starting connection...")

            val jsch = JSch()
            session = jsch.getSession(username, host, port)
            session?.setPassword(password)

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            session?.setConfig(config)

            session?.connect(10000)

            val connected = session?.isConnected == true

            connected

        } catch (e: Exception) {
            Log.e("SSH_TEST", "Connection error: ${e.message}")
            false
        }
    }

    // ---------------- EXECUTE COMMAND ----------------
    fun executeCommand(command: String): String {

        if (session == null || session?.isConnected != true) {
            Log.e("SSH_CMD", "Session not connected")
            return "Not connected"
        }

        return try {

            Log.d("SSH_CMD", "Opening channel")

            val channel =
                session!!.openChannel("exec") as ChannelExec

            channel.setCommand(command)
            channel.inputStream = null
            channel.setErrStream(System.err)

            val inputStream = channel.inputStream

            channel.connect()

            Log.d("SSH_CMD", "Command executing...")

            val reader = BufferedReader(InputStreamReader(inputStream))
            val output = StringBuilder()

            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            channel.disconnect()

            Log.d("SSH_CMD", "Command finished")

            output.toString()

        } catch (e: Exception) {
            Log.e("SSH_CMD", "Execution error: ${e.message}")
            "ERROR: ${e.message}"
        }
    }

    // ---------------- DISCONNECT ----------------
    fun disconnect() {
        session?.disconnect()
        Log.d("SSH_TEST", "Disconnected")
    }
}
