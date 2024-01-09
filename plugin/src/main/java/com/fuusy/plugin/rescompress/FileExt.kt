package com.fuusy.plugin.rescompress

import pink.madis.apk.arsc.StringPoolChunk
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.math.max
import kotlin.math.min

fun ZipFile.unZipFile(path: String) {
    val pathFile = File(path)
    if (!pathFile.exists()) {
        pathFile.mkdirs()
    }
    val zip = this
    val entries = zip.entries()
    while (entries.hasMoreElements()) {
        val entry = entries.nextElement() as ZipEntry
        val zipEntryName = entry.name
        val `in` = zip.getInputStream(entry)
        val outPath = (path + File.separator + zipEntryName).replace("\\*".toRegex(), "/")
        var index = min(outPath.lastIndexOf('/'),outPath.length)
        index = max(index,0)
        val file = File(outPath.substring(0, index))
        if (!file.exists()) {
            file.mkdirs()
        }
        if (File(outPath).isDirectory) {
            continue
        }

        val out = FileOutputStream(outPath)
        `in`.use {
            it.copyTo(out)
        }
        out.close()
    }
}

fun ZipFile.groupsResources(): MutableMap<String, MutableList<ZipEntry>> {
    val groups = mutableMapOf<String, MutableList<ZipEntry>>()
    use { zip ->
        zip
            .entries()
            .iterator()
            .forEach {
                val key = "${it.crc}${File(it.name).parent}"
                val list = if (groups.containsKey(key)) {
                    groups[key]!!
                } else {
                    groups[key] = mutableListOf()
                    groups[key]!!
                }
                list.add(it)
            }
    }
    return groups
}


fun StringPoolChunk.setString(index:Int, value:String){
    try{
        val field = javaClass.getDeclaredField("strings")
        field.isAccessible = true
        val list = field.get(this) as MutableList<String>
        list[index] = value
    }catch (e:Exception){
        e.printStackTrace()
    }
}

@Synchronized
fun FileWriter.synchronizedWriteString(string: String) {
    val prt = "$string${System.getProperty("line.separator")}"
    println(prt)
    write(prt)
}

fun ZipOutputStream.zip(srcRootDir: String, file: File) {
    if (file == null) {
        return
    }
    //如果是文件，则直接压缩该文件
    if (file.isFile) {
        //获取文件相对于压缩文件夹根目录的子路径
        var subPath = file.absolutePath
        val index = subPath.indexOf(srcRootDir)
        if (index != -1) {
            subPath = subPath.substring(srcRootDir.length + File.separator.length)
        }
        subPath = subPath.replace("\\","/")
        val entry = ZipEntry(subPath)
        putNextEntry(entry)

        FileInputStream(file).use {
            it.copyTo(this)
        }
        closeEntry()
    } else {
        //压缩目录中的文件或子目录
        val childFileList = file.listFiles()
        for (n in childFileList.indices) {
            childFileList[n].absolutePath.indexOf(file.absolutePath)
            zip(srcRootDir, childFileList[n])
        }
    }
    //如果是目录，则压缩整个目录

}
