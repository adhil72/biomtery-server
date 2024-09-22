package gecw.cse.utils

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class DownloadManager {
    fun download(url: String, path: String, name: String, async: Boolean) {
        println("Downloading $name from $url to $path")

        if (async) {
            thread { downloadFile(url, path, name) }
        } else {
            downloadFile(url, path, name)
        }
    }

    private fun downloadFile(url: String, path: String, name: String) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val totalSize = connection.contentLengthLong
            var downloadedSize: Long = 0

            connection.inputStream.use { input ->
                File("$path/$name").outputStream().use { output ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedSize += bytesRead
                        val progress = (downloadedSize * 100 / totalSize).toInt()
                        println("Download progress: $progress%")
                    }
                }
            }
            println("Download completed: $name")
        } catch (e: Exception) {
            println("Error downloading file: ${e.message}")
        }
    }
}