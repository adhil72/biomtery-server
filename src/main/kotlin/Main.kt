package gecw.cse

import gecw.ace.db.MongoDbManager
import gecw.ace.webserver.Webserver
import gecw.cse.utils.Terminal
import org.springframework.boot.SpringApplication
import java.io.File

fun main() {
    MongoDbManager.connect()
    File("uploads").mkdirs()
    Terminal.executeCommand("kill -9 3200",true)
    SpringApplication.run(Webserver::class.java)
}