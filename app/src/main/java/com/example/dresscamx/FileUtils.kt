package com.example.dresscamx

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

interface FileUtils {

    fun createDirectoryIfNotExist()
    fun  createFile():File

// fun   createFile(baseFolder: File, format: String, extension: String) =
//    File(baseFolder, SimpleDateFormat(format, Locale.US)
//    .format(System.currentTimeMillis()) + extension)
}