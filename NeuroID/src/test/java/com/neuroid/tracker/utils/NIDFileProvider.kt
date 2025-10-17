package com.neuroid.tracker.utils

import java.io.File

/**
 * File Provider interface to abstract File creation for testing
 */
interface NIDFileProvider {
    fun createFile(parent: String, child: String): File
    fun createFile(pathname: String): File
}

class NIDSystemFileProvider : NIDFileProvider {
    override fun createFile(parent: String, child: String): File = File(parent, child)
    override fun createFile(pathname: String): File = File(pathname)
}

class NIDTestFileProvider(
    private val fileExistenceMap: Map<String, Boolean> = emptyMap()
) : NIDFileProvider {
    
    override fun createFile(parent: String, child: String): File {
        val fullPath = "$parent/$child"
        return MockFile(fullPath, fileExistenceMap[fullPath] ?: false)
    }
    
    override fun createFile(pathname: String): File {
        return MockFile(pathname, fileExistenceMap[pathname] ?: false)
    }
}

/**
 * Simple test File implementation to avoid MockK constructor issues
 */
class MockFile(
    private val path: String,
    private val shouldExist: Boolean
) : File(path) {
    
    override fun exists(): Boolean = shouldExist
    
    override fun getName(): String = path.substringAfterLast('/')
    
    override fun getParent(): String? {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else null
    }
    
    override fun getAbsolutePath(): String = path
}