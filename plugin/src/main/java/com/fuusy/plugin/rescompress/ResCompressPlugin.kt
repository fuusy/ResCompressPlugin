package com.fuusy.plugin.rescompress

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.res.LinkApplicationAndroidResourcesTask
import com.fuusy.plugin.rescompress.util.CompressImgUtil
import com.fuusy.plugin.rescompress.util.CompressImgUtil.optimizeImg
import com.fuusy.plugin.rescompress.util.FileUtil
import kotlinx.coroutines.runBlocking
import org.gradle.api.Plugin
import org.gradle.api.Project
import pink.madis.apk.arsc.ResourceFile
import pink.madis.apk.arsc.ResourceTableChunk
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


class ResCompressPlugin : Plugin<Project> {
    companion object {
        const val RESOURCE_NAME = "resources.arsc"
        const val CONFIG_NAME = "Config"
        const val REPEAT_RES_CONFIG_NAME = "RepeatResConfig"
        const val COMPRESS_IMG_CONFIG_NAME = "CompressImgConfig"

        const val REPEAT_RES_MAPPING = "RepeatResMapping.txt"
        const val COMPRESS_IMG_MAPPING = "CompressIMg.txt"

    }

    override fun apply(p0: Project) {

        // 总配置
        p0.extensions.create(CONFIG_NAME, Config::class.java)
        // 重复资源配置
        p0.extensions.create(REPEAT_RES_CONFIG_NAME, RepeatResConfig::class.java)
        // 压缩图片配置
        p0.extensions.create(COMPRESS_IMG_CONFIG_NAME, CompressImgConfig::class.java)

        val hasAppPlugin = p0.plugins.hasPlugin(AppPlugin::class.java)
        if (hasAppPlugin) {
            p0.afterEvaluate {
                FileUtil.setRootDir(p0.rootDir.path)
                print("PluginTest Config " + p0.extensions.findByName(CONFIG_NAME))
                val config: Config? = p0.extensions.findByName(CONFIG_NAME) as? Config
                val repeatResConfig =
                    p0.extensions.findByName(REPEAT_RES_CONFIG_NAME) as? RepeatResConfig
                val compressImgConfig =
                    p0.extensions.findByName(COMPRESS_IMG_CONFIG_NAME) as? CompressImgConfig

                // 不开启插件
                if (config?.enable == false) {
                    return@afterEvaluate
                }

                val byType = p0.extensions.getByType(AppExtension::class.java)

                byType.applicationVariants.forEach {
                    val variantName = it.name.capitalize()
                    val processRes = p0.tasks.getByName("process${variantName}Resources")
                    processRes.doLast {
                        val resourcesTask =
                            it as LinkApplicationAndroidResourcesTask
                        val files = resourcesTask.resPackageOutputFolder.asFileTree.files
                        files.filter { file ->
                            file.name.endsWith(".ap_")
                        }.forEach { apFile ->
                            val mapping =
                                "${p0.buildDir}${File.separator}ResDeduplication${File.separator}mapping${File.separator}"
                            File(mapping).takeIf { fileMapping ->
                                !fileMapping.exists()
                            }?.apply {
                                mkdirs()
                            }

                            val originalLength = apFile.length()
                            val resCompressFile = File(mapping, REPEAT_RES_MAPPING)
                            val unZipPath = "${apFile.parent}${File.separator}resCompress"
                            ZipFile(apFile).unZipFile(unZipPath)

                            // 删除重复图片
                            deleteRepeatRes(
                                unZipPath,
                                resCompressFile,
                                apFile,
                                repeatResConfig?.whiteListName
                            )
                            // 压缩图片
                            compressImg(mapping, compressImgConfig, unZipPath)
                            apFile.delete()
                            ZipOutputStream(apFile.outputStream()).use { output ->
                                output.zip(unZipPath, File(unZipPath))
                            }

                            val lastLength = apFile.length()
                            print("优化结束缩减：${lastLength - originalLength}")
                            deleteDir(File(unZipPath))
                        }
                    }
                }
            }
        }
    }

    private fun compressImg(mappingDir: String, config: CompressImgConfig?, unZipPath: String) {
        if (config?.enable == true) {
            CompressImgUtil.initTools(config)
            val mappingFile = File(mappingDir, COMPRESS_IMG_MAPPING)
            runBlocking {
                print("开始压缩图片---")
                optimizeImg(mappingFile, config, unZipPath, CopyOnWriteArrayList())
            }
        }
    }


    private fun deleteDir(file: File?): Boolean {
        if (file == null || !file.exists()) {
            return false
        }
        if (file.isFile) {
            file.delete()
        } else if (file.isDirectory) {
            val files = file.listFiles()
            for (i in files.indices) {
                deleteDir(files[i])
            }
        }
        file.delete()
        return true
    }

    private fun deleteRepeatRes(
        unZipPath: String,
        mappingFile: File,
        apFile: File,
        ignoreName: MutableList<String>?
    ) {

        val fileWriter = FileWriter(mappingFile)
        val groupsResources = ZipFile(apFile).groupsResources()

        val arscFile = File(unZipPath, RESOURCE_NAME)
        val newResource = FileInputStream(arscFile).use { input ->
            val fromInputStream = ResourceFile.fromInputStream(input)
            groupsResources.asSequence().filter {
                it.value.size > 1
            }.filter { entry ->
                val name = File(entry.value[0].name).name
                ignoreName?.contains(name)?.let {
                    !it
                } ?: true
            }.forEach { zipMap ->
                val zips = zipMap.value

                val coreResources = zips[0]

                for (index in 1 until zips.size) {

                    val repeatZipFile = zips[index]
                    fileWriter.synchronizedWriteString("${repeatZipFile.name} => ${coreResources.name}")

                    File(unZipPath, repeatZipFile.name).delete()

                    fromInputStream
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
                            val index = stringPoolChunk.indexOf(repeatZipFile.name)
                            if (index != -1) {
                                stringPoolChunk.setString(index, coreResources.name)
                            }
                        }
                }

            }


            fileWriter.close()
            fromInputStream
        }

        arscFile.delete()

        FileOutputStream(arscFile).use {
            it.write(newResource.toByteArray())
        }

    }


}