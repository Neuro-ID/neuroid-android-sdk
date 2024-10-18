package com.neuroid.tracker.utils

import android.content.Context
import android.content.res.AssetManager
import java.io.*

internal fun createJSONFile(
    context: Context,
    fileName: String,
    jsonString: String?,
): Boolean {
    return try {
        // check/create internal folder
        val nidD = File(context.getFilesDir(), Constants.integrationHealthFolder.displayName)
        if (!nidD.exists()) {
            nidD.mkdirs()
        }

        // write to file inside nid folder
        val newFile = File(nidD.path + "/" + fileName)
        val fos: FileOutputStream =
            FileOutputStream(newFile)
        if (jsonString != null) {
            fos.write(jsonString.toByteArray())
        }
        fos.close()

        true
    } catch (fileNotFound: FileNotFoundException) {
//        NIDLog.d("FILE CREATE", "NO FILE WRITTEN")
        false
    } catch (ioException: IOException) {
//        NIDLog.d("FILE CREATE", " IO EXCEPTION FILE WRITTEN")
        false
    }
}

fun copyDirorfileFromAssetManager(
    context: Context,
    arg_assetDir: String,
    arg_destinationDir: String,
): String? {
    val sd_path = context.getFilesDir().absolutePath
    val dest_dir_path: String = sd_path + addLeadingSlash(arg_destinationDir)
    val dest_dir = File(dest_dir_path)
    createDir(dest_dir)
    val asset_manager: AssetManager =
        context.getAssets()
    val files = asset_manager.list(arg_assetDir!!)

    for (i in files!!.indices) {
        val abs_asset_file_path: String = addTrailingSlash(arg_assetDir) + files[i]
        val sub_files = asset_manager.list(abs_asset_file_path)
        if (sub_files!!.isEmpty()) {
//            NIDLog.d("Integration Health", "***** ACTUAL FILE ${files[i].toString()}")
            // It is a file
            val dest_file_path: String = addTrailingSlash(dest_dir_path) + files[i]
            copyAssetFile(context, abs_asset_file_path, dest_file_path)
        } else {
//            NIDLog.d("Integration Health", "***** SUB DIRECTORY ${files[i].toString()}")
            // It is a sub directory
            copyDirorfileFromAssetManager(
                context,
                abs_asset_file_path,
                addTrailingSlash(arg_destinationDir) + files[i],
            )
        }
    }
    return dest_dir_path
}

internal fun copyAssetFile(
    context: Context,
    assetFilePath: String,
    destinationFilePath: String,
) {
    val inS: InputStream = context.getAssets().open(assetFilePath).buffered(1024)
    val out: OutputStream = FileOutputStream(destinationFilePath)
    copyInputStreamToFile(inputStream = inS, outputStream = out)
}

fun copyInputStreamToFile(
    inputStream: InputStream,
    outputStream: OutputStream,
) {
    val buffer = ByteArray(8192)
    inputStream.use { input ->
        outputStream.use { fileOut ->

            while (true) {
                val length = input.read(buffer)
                if (length <= 0) {
                    break
                }
                fileOut.write(buffer, 0, length)
            }
            fileOut.flush()
            fileOut.close()
        }
    }
    inputStream.close()
}

internal fun addTrailingSlash(path: String): String {
    if (path.toCharArray()[path.count() - 1] != '/') {
        return path + "/"
    }
    return path
}

internal fun addLeadingSlash(path: String): String {
    if (path.toCharArray()[0] != '/') {
        return "/" + path
    }
    return path
}

internal fun createDir(dir: File) {
    if (dir.exists()) {
        if (!dir.isDirectory()) {
            throw IOException("Can't create directory, a file is in the way")
        }
    } else {
        dir.mkdirs()
        if (!dir.isDirectory()) {
            throw IOException("Unable to create directory")
        }
    }
}
