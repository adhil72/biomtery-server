package gecw.cse.utils

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FileUtils {
    companion object{
        fun moveFileToFolder(file:File,toFolder:File){
            file.renameTo(File(toFolder,file.name))
        }

        fun moveFolderToFolder(folder:File,toFolder: File){
            folder.renameTo(File(toFolder,folder.name))
        }

        fun deleteFileAfterDelay(file: File, delay: Long, unit: TimeUnit) {
            val scheduler = Executors.newSingleThreadScheduledExecutor()
            scheduler.schedule({
                if (file.exists()) {
                    file.delete()
                    println("File deleted: ${file.name}")
                } else {
                    println("File not found: ${file.name}")
                }
            }, delay, unit)
            scheduler.shutdown()
        }
    }
}