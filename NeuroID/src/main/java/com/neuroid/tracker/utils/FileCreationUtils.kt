package com.neuroid.tracker.utils

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class FileCreationUtils {
    internal fun getFile(path: String, fileName: String) = File(path, fileName)
    internal fun getFileNoPath(fileName: String) = File(fileName)
    internal fun getBufferedReader(inputStreamReader: InputStreamReader) = BufferedReader(inputStreamReader)
}
