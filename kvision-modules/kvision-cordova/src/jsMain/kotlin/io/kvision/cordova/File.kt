@file:Suppress(
    "TooManyFunctions", "TooGenericExceptionCaught"
)

/*
 * Copyright (c) 2017-present Robert Jaros
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.kvision.cordova

import io.kvision.utils.flatMap
import io.kvision.utils.obj
import kotlinx.browser.window
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Exception class for file errors.
 */
class FileException(code: Int) : Exception("Error: $code")

/**
 * System directories class.
 */
external class SystemDirs {
    val applicationDirectory: String
    val applicationStorageDirectory: String
    val dataDirectory: String
    val cacheDirectory: String
    val externalApplicationStorageDirectory: String?
    val externalDataDirectory: String?
    val externalCacheDirectory: String?
    val externalRootDirectory: String?
    val tempDirectory: String?
    val syncedDataDirectory: String?
    val documentsDirectory: String?
    val sharedDirectory: String?
}

/**
 * Main object for Cordova file.
 */
@Deprecated("The kvision-cordova module is deprecated and will be removed in a future version.")
object File {

    const val NOT_FOUND_ERR = 1
    const val SECURITY_ERR = 2
    const val ABORT_ERR = 3
    const val NOT_READABLE_ERR = 4
    const val ENCODING_ERR = 5
    const val NO_MODIFICATION_ALLOWED_ERR = 6
    const val INVALID_STATE_ERR = 7
    const val SYNTAX_ERR = 8
    const val INVALID_MODIFICATION_ERR = 9
    const val QUOTA_EXCEEDED_ERR = 10
    const val TYPE_MISMATCH_ERR = 11
    const val PATH_EXISTS_ERR = 12

    /**
     * File system types.
     */
    enum class FileSystemType {
        TEMPORARY,
        PERSISTENT
    }

    /**
     * Get system directories.
     */
    suspend fun getSystemDirectories(): SystemDirs {
        return suspendCoroutine { continuation ->
            addDeviceReadyListener {
                continuation.resume(window.asDynamic().cordova.file)
            }
        }
    }

    /**
     * Resolve given path to a file or directory entry.
     * @param url directory path
     */
    @Suppress("UnsafeCastFromDynamic")
    suspend fun resolveLocalFileSystemURL(url: String): Result<Entry> {
        return suspendCoroutine { continuation ->
            addDeviceReadyListener {
                window.asDynamic().resolveLocalFileSystemURL(url, { entry ->
                    continuation.resume(Result.success(entry))
                }, { error ->
                    continuation.resume(Result.failure(FileException(error.code)))
                })
            }
        }
    }

    /**
     * Resolve given path to a directory entry.
     * @param url directory path
     */
    suspend fun resolveLocalFileSystemURLForDir(url: String): Result<DirectoryEntry> {
        val result = resolveLocalFileSystemURL(url)
        return if (result.isSuccess) {
            if (result.getOrThrow().isDirectory) {
                result.map {
                    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                    it as DirectoryEntry
                }
            } else {
                Result.failure(FileException(TYPE_MISMATCH_ERR))
            }
        } else {
            result.map {
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                it as DirectoryEntry
            }
        }
    }

    /**
     * Resolve given path to a file entry.
     * @param url file path
     */
    suspend fun resolveLocalFileSystemURLForFile(url: String): Result<FileEntry> {
        val result = resolveLocalFileSystemURL(url)
        return if (result.isSuccess) {
            if (result.getOrThrow().isFile) {
                result.map {
                    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                    it as FileEntry
                }
            } else {
                Result.failure(FileException(TYPE_MISMATCH_ERR))
            }
        } else {
            result.map {
                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                it as FileEntry
            }
        }
    }

    /**
     * Request a file system of a given type.
     * @param fileSystemType file system type (temporary or persistent)
     * @param size file system size (quota)
     */
    @Suppress("UnsafeCastFromDynamic")
    suspend fun requestFileSystem(
        fileSystemType: FileSystemType,
        size: Long = 0
    ): Result<FileSystem> {
        return suspendCoroutine { continuation ->
            val type = when (fileSystemType) {
                FileSystemType.TEMPORARY -> LocalFileSystem.TEMPORARY
                FileSystemType.PERSISTENT -> LocalFileSystem.PERSISTENT
            }
            addDeviceReadyListener {
                window.asDynamic().requestFileSystem(type, size, { fs ->
                    continuation.resume(Result.success(fs))
                }, { error ->
                    continuation.resume(Result.failure(FileException(error.code)))
                })
            }
        }
    }

    internal fun dataURLtoBlob(dataURI: String): Blob {
        val byteString = window.atob(dataURI.split(',')[1])
        val mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0]
        val ab = ArrayBuffer(byteString.length)
        val ia = Uint8Array(ab)
        for (i in byteString.indices) {
            ia[i] = byteString[i].code.toByte()
        }
        return Blob(arrayOf(ab), BlobPropertyBag(type = mimeString))
    }

}

/**
 * Extension function to convert String to a directory entry.
 */
suspend fun String.toDirectoryEntry(): Result<DirectoryEntry> {
    return File.resolveLocalFileSystemURLForDir(this)
}

/**
 * Extension function to convert String to a file entry.
 */
suspend fun String.toFileEntry(): Result<FileEntry> {
    return File.resolveLocalFileSystemURLForFile(this)
}

/**
 * Get file or directory metadata.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun Entry.getMetadata(): Result<Metadata> {
    return suspendCoroutine { continuation ->
        this.getMetadata({ metadata: Metadata ->
            continuation.resume(Result.success(metadata))
        } as MetadataCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Get file or directory parent directory entry.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun Entry.getParent(): Result<DirectoryEntry> {
    return suspendCoroutine { continuation ->
        this.getParent({ directoryEntry: DirectoryEntry ->
            continuation.resume(Result.success(directoryEntry))
        } as DirectoryEntryCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Remove given file or directory.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun Entry.remove(): Result<Entry> {
    return suspendCoroutine { continuation ->
        this.remove({
            continuation.resume(Result.success(this))
        } as VoidCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Move given file or directory to a new location.
 * @param parent new location parent directory entry
 * @param newName new location name
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun Entry.moveTo(parent: DirectoryEntry, newName: String? = null): Result<Entry> {
    return suspendCoroutine { continuation ->
        this.moveTo(parent, newName, { entry: Entry ->
            continuation.resume(Result.success(entry))
        } as EntryCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Copy given file or directory to a new location.
 * @param parent new location parent directory entry
 * @param newName new location name
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun Entry.copyTo(parent: DirectoryEntry, newName: String? = null): Result<Entry> {
    return suspendCoroutine { continuation ->
        this.copyTo(parent, newName, { entry: Entry ->
            continuation.resume(Result.success(entry))
        } as EntryCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Create a FileWriter object for a given file entry.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun FileEntry.createWriter(): Result<FileWriter> {
    return suspendCoroutine { continuation ->
        this.createWriter({ fileWriter: FileWriter ->
            continuation.resume(Result.success(fileWriter))
        } as FileWriterCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Get a File object for a given file entry.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun FileEntry.file(): Result<org.w3c.files.File> {
    return suspendCoroutine { continuation ->
        this.file({ file: org.w3c.files.File ->
            continuation.resume(Result.success(file))
        } as FileCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Get or create a file in a given parent directory.
 * @param path target file path
 * @param create Used to indicate that the user wants to create a file if it was not previously there.
 * @param exclusive Fail if the target path file already exists.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun DirectoryEntry.getFile(
    path: String,
    create: Boolean = true,
    exclusive: Boolean = false
): Result<FileEntry> {
    return suspendCoroutine { continuation ->
        this.getFile(path, obj {
            this.create = create
            this.exclusive = exclusive
        }, { fileEntry: FileEntry ->
            continuation.resume(Result.success(fileEntry))
        } as FileEntryCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Get or create a directory in a given parent directory.
 * @param path target directory path
 * @param create Used to indicate that the user wants to create a directory if it was not previously there.
 * @param exclusive Fail if the target path directory already exists.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun DirectoryEntry.getDirectory(
    path: String,
    create: Boolean = true,
    exclusive: Boolean = false
): Result<DirectoryEntry> {
    return suspendCoroutine { continuation ->
        this.getDirectory(path, obj {
            this.create = create
            this.exclusive = exclusive
        }, { directoryEntry: DirectoryEntry ->
            continuation.resume(Result.success(directoryEntry))
        } as DirectoryEntryCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * Remove given directory recursively.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun DirectoryEntry.removeRecursively(): Result<DirectoryEntry> {
    return suspendCoroutine { continuation ->
        this.removeRecursively({
            continuation.resume(Result.success(this))
        } as VoidCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * List directory entries for a given DirectoryReader.
 */
@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE", "UnsafeCastFromDynamic")
suspend fun DirectoryReader.readEntries(): Result<List<Entry>> {
    return suspendCoroutine { continuation ->
        this.readEntries({ entries: Array<Entry> ->
            continuation.resume(Result.success(entries.toList()))
        } as EntriesCallback, { error: dynamic ->
            continuation.resume(Result.failure(FileException(error.code)))
        } as ErrorCallback)
    }
}

/**
 * List directory entries for a given parent directory entry.
 */
suspend fun DirectoryEntry.readEntries(): Result<List<Entry>> {
    return this.createReader().readEntries()
}

/**
 * Read file content as a plain string.
 */
@Suppress("UnsafeCastFromDynamic")
suspend fun FileEntry.readAsText(): Result<String> {
    return this.file().flatMap { file ->
        suspendCoroutine { continuation ->
            val reader = FileReader()
            reader.onloadend = { e ->
                continuation.resume(Result.success(e.target.asDynamic().result))
            }
            reader.onerror = { error: dynamic ->
                continuation.resume(Result.failure(FileException(error.code)))
            }
            reader.readAsText(file)
        }
    }
}

/**
 * Read file content as a data url.
 */
@Suppress("UnsafeCastFromDynamic")
suspend fun FileEntry.readAsDataURL(): Result<String> {
    return this.file().flatMap { file ->
        suspendCoroutine { continuation ->
            val reader = FileReader()
            reader.onloadend = { e ->
                continuation.resume(Result.success(e.target.asDynamic().result))
            }
            reader.onerror = { error: dynamic ->
                continuation.resume(Result.failure(FileException(error.code)))
            }
            reader.readAsDataURL(file)
        }
    }
}

/**
 * Read file content as an array buffer.
 */
@Suppress("UnsafeCastFromDynamic")
suspend fun FileEntry.readAsArrayBuffer(): Result<ArrayBuffer> {
    return this.file().flatMap { file ->
        suspendCoroutine { continuation ->
            val reader = FileReader()
            reader.onloadend = { e ->
                continuation.resume(Result.success(e.target.asDynamic().result))
            }
            reader.onerror = { error: dynamic ->
                continuation.resume(Result.failure(FileException(error.code)))
            }
            reader.onabort = { _ ->
                continuation.resume(Result.failure(FileException(File.ABORT_ERR)))
            }
            reader.readAsArrayBuffer(file)
        }
    }
}

/**
 * Write file content from a Blob.
 * @param data a data Blob to be written.
 */
@Suppress("UnsafeCastFromDynamic")
suspend fun FileEntry.write(data: Blob): Result<FileEntry> {
    return this.createWriter().flatMap { writer ->
        suspendCoroutine { continuation ->
            writer.onwriteend = {
                continuation.resume(Result.success(this))
            }
            writer.onerror = { error: dynamic ->
                continuation.resume(Result.failure(FileException(error.code)))
            }
            writer.onabort = { _ ->
                continuation.resume(Result.failure(FileException(File.ABORT_ERR)))
            }
            writer.write(data)
        }
    }
}

/**
 * Write file content from a plain string.
 * @param data a data string to be written.
 */
suspend fun FileEntry.write(data: String): Result<FileEntry> {
    return this.write(Blob(arrayOf(data)))
}

/**
 * Write file content from a data url.
 * @param dataUrl a data url to be written.
 */
suspend fun FileEntry.writeDataUrL(dataUrl: String): Result<FileEntry> {
    return this.write(File.dataURLtoBlob(dataUrl))
}

/**
 * Write file content from an array buffer.
 * @param data an array buffer to be written.
 */
suspend fun FileEntry.write(data: ArrayBuffer): Result<FileEntry> {
    return this.write(data.toBlob())
}

/**
 * Append file content from a Blob.
 * @param data a data Blob to be appended.
 */
@Suppress("UnsafeCastFromDynamic")
suspend fun FileEntry.append(data: Blob): Result<FileEntry> {
    return this.createWriter().flatMap { writer ->
        try {
            writer.seek(writer.length)
        } catch (e: Exception) {
            console.log("File doesn't exist!")
        }
        suspendCoroutine { continuation ->
            writer.onwriteend = {
                continuation.resume(Result.success(this))
            }
            writer.onerror = { error: dynamic ->
                continuation.resume(Result.failure(FileException(error.code)))
            }
            writer.onabort = { _ ->
                continuation.resume(Result.failure(FileException(File.ABORT_ERR)))
            }
            writer.write(data)
        }
    }
}

/**
 * Append file content from a plain string.
 * @param data a string to be appended.
 */
suspend fun FileEntry.append(data: String): Result<FileEntry> {
    return this.append(Blob(arrayOf(data)))
}

/**
 * Convert array buffer to a blob.
 */
fun ArrayBuffer.toBlob(): Blob {
    return Blob(arrayOf(Uint8Array(this)))
}
