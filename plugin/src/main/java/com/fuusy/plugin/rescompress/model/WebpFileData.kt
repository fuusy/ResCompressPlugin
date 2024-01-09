package com.fuusy.plugin.rescompress.model

import java.io.File

data class WebpFileData(val original: File, val webpFile: File, val reduceSize: Long)
