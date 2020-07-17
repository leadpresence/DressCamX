package com.example.dresscamx



import android.os.Environment
import androidx.core.content.ContentProviderCompat.requireContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileUtilsImpl : FileUtils {

    companion object {

        private const val IMAGE_PREFIX = "dresscamx_"
        private const val JPG_SUFFIX = ".jpg"
        private const val FOLDER_NAME = "Dresscamx"
    }

    override fun createDirectoryIfNotExist() {
        val folder = File(Environment.getExternalStorageDirectory().toString() +
                File.separator + Environment.DIRECTORY_PICTURES + File.separator + FOLDER_NAME)
        if (!folder.exists()) {
            folder.mkdirs()
        }
    }

    override fun createFile() = File(
        Environment.getExternalStorageDirectory().toString() +
                File.separator + Environment.DIRECTORY_PICTURES + File.separator +
                FOLDER_NAME + File.separator + IMAGE_PREFIX + System.currentTimeMillis() + JPG_SUFFIX
    )

}