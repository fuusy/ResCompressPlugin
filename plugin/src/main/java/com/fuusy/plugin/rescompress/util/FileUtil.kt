package com.fuusy.plugin.rescompress.util

import java.io.File


object FileUtil {
    const val PATH_NAME = "SystemTools"

    private lateinit var rootDir: String

    fun setRootDir(rootDir: String) {
        FileUtil.rootDir = rootDir
    }

    fun getRootDirPath(): String {
        return rootDir
    }

    fun getToolsDir(): File {
        return File("$rootDir/$PATH_NAME/")
    }

    fun getToolsDirPath(): String {
        return "$rootDir/$PATH_NAME/"
    }
}