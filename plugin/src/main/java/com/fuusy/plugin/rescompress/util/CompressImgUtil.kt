package com.fuusy.plugin.rescompress.util

import com.fuusy.plugin.rescompress.CompressImgConfig
import com.fuusy.plugin.rescompress.CompressImgConfig.Companion.OPTIMIZE_COMPRESS_PICTURE
import com.fuusy.plugin.rescompress.CompressImgConfig.Companion.OPTIMIZE_WEBP_CONVERT
import com.fuusy.plugin.rescompress.model.WebpFileData
import com.fuusy.plugin.rescompress.setString
import com.fuusy.plugin.rescompress.synchronizedWriteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.gradle.api.GradleException
import pink.madis.apk.arsc.ResourceFile
import pink.madis.apk.arsc.ResourceTableChunk
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.concurrent.CopyOnWriteArrayList


object CompressImgUtil {

    /**
    优化图片
     */
    suspend fun CoroutineScope.optimizeImg(
        mappingFile: File,
        config: CompressImgConfig,
        unZipDir: String,
        webpsLsit: CopyOnWriteArrayList<WebpFileData>
    ) {
        compressionImg(mappingFile, unZipDir, config, webpsLsit)

        if (webpsLsit.size > 0) {
            modifyResources(webpsLsit, unZipDir)
        }
    }

    private fun modifyResources(
        webpOkList: CopyOnWriteArrayList<WebpFileData>,
        unZipDir: String
    ) {
        // 开始修改 webp 的 resources 路径
        val resourcesFile = File(unZipDir, "resources.arsc")

        val newResouce = FileInputStream(resourcesFile).use {
            val resouce = ResourceFile.fromInputStream(it)
            // 变量修改
            webpOkList.forEach { webpFile ->
                resouce
                    .chunks
                    .asSequence()
                    .filter {
                        it is ResourceTableChunk
                    }
                    .map {
                        it as ResourceTableChunk
                    }
                    .forEach { chunk ->
                        val stringPoolChunk = chunk.stringPool
                        // 注意当前的 original 和 webpFile 路径是当前电脑的绝对路径 需要转化成res/开头的路径
                        val originalPath = webpFile.original.absolutePath.replace(
                            "${unZipDir}${File.separator}",
                            ""
                        )
                        val index = stringPoolChunk.indexOf(originalPath)

                        if (index != -1) {
                            // 进行剔除重复资源
                            val webpPath = webpFile.webpFile.absolutePath.replace(
                                "${unZipDir}${File.separator}",
                                ""
                            )
                            stringPoolChunk.setString(index, webpPath)
                        }
                    }
            }
            resouce
        }

        // 修改完成
        resourcesFile.delete()
        FileOutputStream(resourcesFile).use {
            it.write(newResouce.toByteArray())
        }

    }

    /**
     * 压缩图片
     */
    private suspend fun CoroutineScope.compressionImg(
        mappingFile: File,
        unZipDir: String,
        config: CompressImgConfig,
        webpsList: CopyOnWriteArrayList<WebpFileData>
    ) {
        kotlin.runCatching {
            val mappginWriter = FileWriter(mappingFile)
            launch {
                // 查找所有的图片
                val file = File("$unZipDir${File.separator}res")
                file.listFiles()
                    .filter {
                        it.isDirectory && (it.name.startsWith("drawable") || it.name.startsWith("mipmap"))
                    }
                    .flatMap {
                        it.listFiles().toList()
                    }
                    .asSequence()
                    .filter {
                        config.whiteListName?.contains(it.name)?.let { !it } ?: true
                    }
                    .filter {
                        ImageUtil.isImage(it)
                    }
                    .forEach {
                        // 进行图片压缩
                        launch(Dispatchers.Default) {
                            when (config.optimizeType) {

                                OPTIMIZE_COMPRESS_PICTURE -> {
                                    val originalPath =
                                        it.absolutePath.replace("${unZipDir}${File.separator}", "")
                                    val reduceSize = compressImg(it)
                                    if (reduceSize > 0) {
                                        mappginWriter.synchronizedWriteString("$originalPath => 减少[$reduceSize]")
                                    } else {
                                        mappginWriter.synchronizedWriteString("$originalPath => 压缩失败")
                                    }
                                }

                                OPTIMIZE_WEBP_CONVERT -> {
                                    val webp0K = ImageUtil.securityFormatWebp(it, config)
                                    // 加入可以的 webbp 路径
                                    webp0K?.apply {
                                        val originalPath = original.absolutePath.replace(
                                            "${unZipDir}${File.separator}",
                                            ""
                                        )
                                        val webpFilePath = webpFile.absolutePath.replace(
                                            "${unZipDir}${File.separator}",
                                            ""
                                        )
                                        mappginWriter.synchronizedWriteString("$originalPath => $webpFilePath => 减少[$reduceSize]")
                                        webpsList.add(this)
                                    }
                                }

                                else -> {
                                }
                            }
                        }
                    }


            }.join()
            mappginWriter.close()
        }

    }


    fun initTools() {
        if (Tools.isLinux()) {
            Tools.chmod()
        }
        // 判断图片压缩文件是否存在

        val osName = Tools.getOsName() ?: throw GradleException("操作系统未知!")

        val tools = File("${FileUtil.getToolsDirPath()}/$osName")
        if (!tools.exists()) {
            throw GradleException("请将 SystemTools 目录拷贝到 ${FileUtil.getRootDirPath()} 下")
        }

        if (Tools.isMac() || Tools.isLinux()) {
            // 检测文件是否存在
            val toolsName = arrayOf("cwebp", "guetzli", "pngquant")
            val all = toolsName.all {
                File("$tools${File.separator}$it").exists()
            }
            if (!all) {
                throw GradleException("请将目录拷贝到 ${FileUtil.getRootDirPath()} 下")
            }
        } else if (Tools.isWindows()) {
            val toolsName = arrayOf("cwebp.exe", "guetzli.exe", "pngquant.exe")
            val all = toolsName.all {
                File("${tools.parentFile}${File.separator}$it").exists()
            }
            if (!all) {
                throw GradleException("请将 SystemTools 目录拷贝到 ${FileUtil.getRootDirPath()} 下")
            }
        }


    }


    fun compressImg(imgFile: File): Long {
        if (!ImageUtil.isImage(imgFile)) {
            return 0
        }
        val oldSize = imgFile.length()
        val newSize: Long
        if (ImageUtil.isJPG(imgFile)) {
            val tempFilePath: String =
                "${imgFile.path.substring(0, imgFile.path.lastIndexOf("."))}_temp" +
                        imgFile.path.substring(imgFile.path.lastIndexOf("."))
            Tools.cmd("guetzli", "${imgFile.path} $tempFilePath")
            val tempFile = File(tempFilePath)
            newSize = tempFile.length()
            return if (newSize < oldSize) {
                val imgFileName: String = imgFile.path
                if (imgFile.exists()) {
                    imgFile.delete()
                }
                tempFile.renameTo(File(imgFileName))
                oldSize - newSize
            } else {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                0L
            }

        } else {
            Tools.cmd(
                "pngquant",
                "--skip-if-larger --speed 1 --nofs --strip --force --output ${imgFile.path} -- ${imgFile.path}"
            )
            newSize = File(imgFile.path).length()
        }

        print("compressImg ${imgFile.path} oldSize = $oldSize newSize = $newSize")
        return oldSize - newSize
    }

}