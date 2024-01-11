package com.fuusy.plugin.rescompress.util

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader


object Tools {
    private const val TAG = "Tools"

    fun cmd(cmd: String, params: String) {
        val cmdStr = if (isCmdExist(cmd)) {
            "$cmd $params"
        } else {
            val system = System.getProperty("os.name")
            when (system) {
                "Mac OS X" ->
                    FileUtil.getToolsDirPath() + "mac/" + "$cmd $params"
                "Linux" ->
                    FileUtil.getToolsDirPath() + "linux/" + "$cmd $params"
                "Windows" ->
                    FileUtil.getToolsDirPath() + "windows/" + "$cmd $params"
                else -> ""
            }
        }
        if (cmdStr == "") {
            return
        }
        outputMessage(cmdStr)
    }

    fun isLinux(): Boolean {
        val system = System.getProperty("os.name")
        return system.startsWith("Linux")
    }

    fun isMac(): Boolean {
        val system = System.getProperty("os.name")
        return system.startsWith("Mac OS")
    }

    fun isWindows(): Boolean {
        val system = System.getProperty("os.name")
        return system.startsWith("Windows")
    }


    fun getOsName(): String? {
        val system = System.getProperty("os.name")
        print("--- getOsName $system")
        return when (system) {
            "Mac OS X" ->
                "mac"
            "Linux" ->
                "linux"
            "Windows" ->
                "windows"
            else -> null
        }
    }

    fun chmod() {
        // outputMessage("chmod 755 -R ${FileUtil.getRootDirPath()}")
    }

    fun chmod755(file: File) {
        if (file.isFile) {
            outputMessage("chmod 755 -R ${file.absolutePath}")
        } else {
            outputMessage("chmod 755 -R ${file.absolutePath}")
            file.listFiles().forEach {
                chmod755(it)
            }
        }
    }

    private fun outputMessage(cmd: String) {
        val process = Runtime.getRuntime().exec(cmd)
        process.waitFor()
    }

    private fun isCmdExist(cmd: String): Boolean {
        val result = if (isMac() || isLinux()) {
            executeCmd("which $cmd")
        } else {
            executeCmd("where $cmd")
        }
        return !result.isNullOrEmpty()
    }

    private fun executeCmd(cmd: String): String? {
        val process = Runtime.getRuntime().exec(cmd)
        process.waitFor()
        val bufferReader = BufferedReader(InputStreamReader(process.inputStream))
        return try {
            bufferReader.readLine()
        } catch (e: Exception) {
            null
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun macSudo(pass: String, cmd: String) {
        val process = Runtime.getRuntime().exec(arrayOf("/bin/bash", "-c", "echo \"$pass\" | sudo -S $cmd"))
//            val ir = InputStreamReader(process.inputStream)
        process.waitFor()

    }
}