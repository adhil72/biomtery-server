package gecw.cse.utils

import com.machinezoo.sourceafis.FingerprintImage
import com.machinezoo.sourceafis.FingerprintImageOptions
import com.machinezoo.sourceafis.FingerprintMatcher
import com.machinezoo.sourceafis.FingerprintTemplate
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*


class FingerPrintScanner {

    companion object {
        var process: Process? = null
    }

    fun scan(folder: File, fileName: String): String? {
        return exec(File("driver").absolutePath, folder, fileName)
    }

    private fun exec(cmd: String, folder: File, fileName: String): String? {
        File("prints").mkdir()
        if (process != null) {
            process!!.destroy()
        }
        try {
            java.awt.Toolkit.getDefaultToolkit().beep()

            process = Runtime.getRuntime().exec(cmd)
            process?.waitFor()

            val reader = BufferedReader(InputStreamReader(process?.inputStream!!))
            var line: String? = ""
            while ((reader.readLine().also { line = it }) != null) {
                println(line)
            }

            var imgFile = File("frame_Ex.bmp")
            val fileNameExt = "$fileName.bmp"
            renameFile(imgFile, fileNameExt)
            imgFile = File(fileNameExt)
            moveFileToFolder(imgFile, folder)
            return fileNameExt
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getCurrentTimeString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
        val date: Date = Date()
        return formatter.format(date)
    }

    private fun renameFile(file: File, toName: String) {
        if (file.exists()) {
            val newFile = File(file.parent, toName)
            if (file.renameTo(newFile)) {
                println("File renamed to: $toName")
            } else {
                println("Failed to rename file.")
            }
        } else {
            println("File does not exist.")
        }
    }

    private fun moveFileToFolder(file: File, folder: File) {
        if (file.exists() && folder.exists()) {
            val newFile = File(folder, file.name)
            if (file.renameTo(newFile)) {
                println("File moved to: " + folder.name)
            } else {
                println("Failed to move file.")
            }
        } else {
            println("File or folder does not exist.")
        }
    }

    fun matchFingerprint(p1: File, p2: File): Boolean {
        val probe = FingerprintTemplate(
            FingerprintImage(
                Files.readAllBytes(Paths.get(p1.path)), FingerprintImageOptions().dpi(500.0)
            )
        )
        val candidate = FingerprintTemplate(
            FingerprintImage(
                Files.readAllBytes(
                    Paths.get(
                        p2.path
                    )
                ), FingerprintImageOptions().dpi(500.0)
            )
        )
        val score: Double = FingerprintMatcher(probe).match(candidate)
        return score >= 40
    }

    fun detectFingerprintInFolder(folder: File, print: File): String {
        var fingerprintFiles = folder.listFiles { dir: File?, name: String -> name.endsWith(".bmp") }
        if (fingerprintFiles == null) fingerprintFiles = arrayOf()
        fingerprintFiles.forEach { if (matchFingerprint(print, it)) return it.parentFile.name }
        return "NA"
    }

    fun detectFingerprint(print: File, sem: String): String {
        val printsFolder = File("fingerprints", sem)
        printsFolder.listFiles()?.forEach {
            val uid = detectFingerprintInFolder(it, print)
            if (uid != "NA") return uid
        }
        return "NA"
    }

    fun validateFingerprint(uid: String): Boolean {
        scan(File(""), "test")
        val folder = File("prints", uid)
        val fingerprintFiles = folder.listFiles { dir: File?, name: String ->
            name.endsWith(
                ".bmp"
            )
        }

        for (i in fingerprintFiles.indices) {
            val probe = FingerprintTemplate(
                FingerprintImage(
                    Files.readAllBytes(Paths.get(File("test.bmp").path)), FingerprintImageOptions().dpi(500.0)
                )
            )
            val candidate = FingerprintTemplate(
                FingerprintImage(
                    Files.readAllBytes(
                        Paths.get(
                            fingerprintFiles[i].path
                        )
                    ), FingerprintImageOptions().dpi(500.0)
                )
            )
            val score: Double = FingerprintMatcher(probe).match(candidate)
            val matches = score >= 40
            if (matches) {
                return true
            }
        }
        return false
    }
}