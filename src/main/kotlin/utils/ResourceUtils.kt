package gecw.cse.utils

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

fun getResourceAsStream(resourcePath: String): InputStream? {
    return try {
        val resource: Resource = ClassPathResource(resourcePath)
        resource.inputStream
    } catch (e: IOException) {
        e.printStackTrace() // Handle the exception as needed
        null
    }
}

fun extractResource(resourcePath: String, destinationPath: String) {
    try {
        val inputStream: InputStream = getResourceAsStream(resourcePath)!!
        FileOutputStream(destinationPath).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}