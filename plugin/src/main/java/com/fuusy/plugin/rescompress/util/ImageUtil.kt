package com.fuusy.plugin.rescompress.util

import com.fuusy.plugin.rescompress.CompressImgConfig
import com.fuusy.plugin.rescompress.model.WebpFileData
import java.io.File
import java.io.FileInputStream
import javax.imageio.ImageIO


object ImageUtil {

    private const val TAG = "ImageUtil"
    private const val JPG = ".jpg"
    private const val JPEG = ".jpeg"
    private const val PNG = ".png"
    private const val DOT_9PNG = ".9.png"

    fun isImage(file: File): Boolean {
        return (file.name.endsWith(JPG) ||
                file.name.endsWith(PNG) ||
                file.name.endsWith(JPEG)
                ) && !file.name.endsWith(DOT_9PNG)
    }

    fun isJPG(file: File): Boolean {
        return file.name.endsWith(JPG) || file.name.endsWith(JPEG)
    }

    private fun isAlphaPNG(filePath: File): Boolean {
        return if (filePath.exists()) {
            try {
                val img = ImageIO.read(filePath)
                img.colorModel.hasAlpha()
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    private fun formatWebp(imgFile: File): WebpFileData? {
        if (isImage(imgFile)) {
            val webpFile = File("${imgFile.path.substring(0, imgFile.path.lastIndexOf("."))}.webp")
            Tools.cmd("cwebp", "${imgFile.path} -o ${webpFile.path} -m 6 -quiet")
            val reduceSize = imgFile.length() - webpFile.length()

            return if (reduceSize > 0) {
                if (imgFile.exists()) {
                    imgFile.delete()
                }
                WebpFileData(imgFile, webpFile, reduceSize)
            } else {
                //如果webp的大的话就抛弃
                if (webpFile.exists()) {
                    webpFile.delete()
                }
                print("webp 过大${imgFile.name}  ${webpFile.absolutePath}")
                null
            }
        }
        return null
    }

    fun securityFormatWebp(imgFile: File, config: CompressImgConfig): WebpFileData? {
        if (isImage(imgFile)) {
            if (config.supportAlphaWebp) {
                return formatWebp(imgFile)
            } else {
                if (imgFile.name.endsWith(JPG) || imgFile.name.endsWith(JPEG)) {
                    //jpg
                    return formatWebp(imgFile)
                } else if (imgFile.name.endsWith(PNG)) {
                    //png
                    if (!isAlphaPNG(imgFile)) {
                        //不包含透明通道
                        return formatWebp(imgFile)
                    } else {
                        //包含透明通道的png，进行压缩
                        CompressImgUtil.compressImg(imgFile)
                    }
                }
            }
        }
        return null
    }

}