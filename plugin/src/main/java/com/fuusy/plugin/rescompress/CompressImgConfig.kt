package com.fuusy.plugin.rescompress

/**
 * Created on 2024/1/8.
 * @author shiyao.fu
 * @email shiyao.fu@ximalaya.com
 */
open class CompressImgConfig {
    val enable  = true
    val whiteListName: MutableList<String>? = null // 白名单
    var supportAlphaWebp = false

    var optimizeType = OPTIMIZE_COMPRESS_PICTURE
    var pass = ""


    companion object {
        //webp化
        const val OPTIMIZE_WEBP_CONVERT = "ConvertWebp"
        //压缩图片
        const val OPTIMIZE_COMPRESS_PICTURE = "Compress"
    }

}