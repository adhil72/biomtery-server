package gecw.ace.webserver

import gecw.ace.db.MongoDbManager
import gecw.cse.utils.DateUtils
import gecw.cse.utils.FileUtils
import gecw.cse.utils.FingerPrintScanner
import gecw.cse.utils.Uid
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.bson.BsonArray
import org.bson.BsonBoolean
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.Document
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api")
class Routes {

    @GetMapping("/generate/uid")
    fun generateUid(): String {
        val uid = Uid.generate()
        val f = File("prints", uid)
        f.mkdirs()
        FileUtils.deleteFileAfterDelay(f, 10, TimeUnit.MINUTES)
        return BsonDocument().apply {
            append("uid", BsonString(uid))
        }.toString()
    }

    @PostMapping("/finger/create")
    fun createFinger(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("uid") uid: String,
        @RequestParam("count") count: String
    ): String {
        println("Creating fingerprint for $uid")
        return try {
            val uidPath = File("prints", uid)
            uidPath.mkdirs()
            val filePath = File(uidPath, "$count.bmp")

            file.inputStream.use { input ->
                filePath.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            "{}"
        } catch (e: Exception) {
            e.printStackTrace()
            BsonDocument().apply {
                append("error", BsonString(e.message))
            }.toString()
        }
    }

    @PostMapping("/finger/verify")
    fun verifyFinger(@RequestParam("file") file: MultipartFile, @RequestParam("uid") uid: String): String {
        val uploadDir = File("uploads")
        val filePath = File(uploadDir, "verify_${UUID.randomUUID()}.bmp")
        file.inputStream.use { input ->
            filePath.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val uidFolder = File("prints", uid)
        val matchedUid = FingerPrintScanner().detectFingerprintInFolder(uidFolder, filePath)
        filePath.delete()
        return BsonDocument().apply {
            append("uid", BsonString(matchedUid))
            append("match", BsonBoolean(matchedUid == uid))
        }.toString()
    }

    @PostMapping("/create/user")
    fun createEntry(@RequestBody body: String): String {
        val data = BsonDocument.parse(body)
        val uid = data.getString("uid").value
        val name = data.getString("name").value
        val rollNumber = data.getString("rollNumber").value
        val semester = data.getString("semester").value

        var semFolder = File("fingerprints", semester)
        semFolder.mkdirs()
        semFolder = File(semFolder, uid)
        val printsFolder = File("prints", uid)
        printsFolder.copyRecursively(semFolder, true)
        printsFolder.deleteRecursively()

        MongoDbManager.mongoClient.getDatabase("ace").getCollection("users").insertOne(Document().apply {
            append("_id", uid)
            append("name", name)
            append("rollNumber", rollNumber)
            append("semester", semester)
            append("created_at", Date())
            append("updated_at", Date())
        })

        return BsonDocument().apply {
            append("status", BsonString("success"))
        }.toString()
    }

    @GetMapping("/finger/terminate")
    fun terminateFinger(): String {
        println(FingerPrintScanner.process == null)
        FingerPrintScanner.process?.destroy()
        return BsonDocument().apply {
            append("status", BsonString("success"))
        }.toString()
    }


    @PostMapping("/finger/detect")
    fun detectFinger(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("sem") sem: String,
        @RequestParam("type") type: String,
        @RequestParam("session") session: String
    ): String {
        return try {
            val uploadDir = File("uploads")
            val filePath = File(uploadDir, "detect_${UUID.randomUUID()}.bmp")
            file.inputStream.use { input ->
                filePath.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val uid = FingerPrintScanner().detectFingerprint(filePath, sem)
            filePath.delete()

            val user =
                MongoDbManager.mongoClient.getDatabase("ace").getCollection("users").find(Document("_id", uid)).first()
            if (user != null) {
                val todayLog =
                    MongoDbManager.mongoClient.getDatabase("ace").getCollection("logs").find(Document().apply {
                        append("uid", uid)
                        append("type", type)
                        append("session", session)
                        append("time", Document().apply {
                            append("\$gte", Date().apply {
                                hours = 0
                                minutes = 0
                                seconds = 0
                            })
                        })
                    }).first()

                println(todayLog)

                if (todayLog != null) return user.apply {
                    append("match", BsonBoolean(true))
                }.toJson().toString()

                MongoDbManager.mongoClient.getDatabase("ace").getCollection("logs").insertOne(Document().apply {
                    append("uid", uid)
                    append("time", Date())
                    append("type", type)
                    append("session", session)
                })

                return user.apply {
                    append("match", true)
                }.toJson().toString()
            } else {
                return BsonDocument().apply {
                    append("uid", BsonString(uid))
                    append("match", BsonBoolean(false))
                }.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BsonDocument().apply {
                append("error", BsonString(e.message))
                append("match", BsonBoolean(false))
            }.toString()
        }
    }

    @GetMapping("/records")
    fun getAllRecords(@RequestParam date: String, @RequestParam sem: String, @RequestParam session: String): String {
        val users = MongoDbManager.mongoClient.getDatabase("ace").getCollection("users").find(Document().apply {
            append("semester", sem)
        }).toList()

        if (users.isEmpty()) return "{data:[]}"

        var jsonData = "["
        println(date)
        for (user in users) {
            val logs = MongoDbManager.mongoClient.getDatabase("ace").getCollection("logs").find(Document().apply {
                append("uid", user.getString("_id"))
                append("time", Document().apply {
                    append("\$gte", DateUtils.isoStringToDate("${date}T00:00:00.000Z"))
                    append("\$lt", DateUtils.isoStringToDate("${date}T23:59:59.999Z"))
                })
                append("session", session)
            }).toList()
            user.append("logs", logs)
            jsonData += user.toJson() + ","
        }
        jsonData = jsonData.substring(0, jsonData.length - 1) + "]"
        return "{data:$jsonData}"
    }

    @GetMapping("/export/json")
    fun exportStudentsAsJson(@RequestParam sem: String): ResponseEntity<ByteArrayResource> {
        val users = MongoDbManager.mongoClient.getDatabase("ace").getCollection("users").find(Document().apply {
            append("semester", sem)
        }).toList()

        val json = if (users.isEmpty()) "[]" else users.joinToString(
            separator = ",", prefix = "[", postfix = "]"
        ) { it.toJson() }

        val f = File("temps", "students_${Uid.generate()}_$sem.json")
        f.writeText(json)

        val resource = ByteArrayResource(f.readBytes())

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students_${Uid.generate()}_$sem.json")
            .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(f.length()).body(resource)
    }

    @GetMapping("/export/csv")
    fun exportStudentsAsCsv(
        @RequestParam sem: String,
        @RequestParam session: String,
        @RequestParam date: String
    ): ResponseEntity<ByteArrayResource> {
        println(getAllRecords(date, sem, session))
        var records = "Name, Roll number, Semester, Session, In time, Out time"
        BsonDocument.parse(getAllRecords(date, sem, session))["data"]?.asArray()?.forEach {
            val logs = it.asDocument()["logs"]?.asArray()
            var inTime = "Na"
            var outTime = "Na"
            logs?.forEach {
                if (it.asDocument()["type"]?.asString()?.value == "in") {
                    val timeInSeconds = it.asDocument()["time"]?.asDateTime()?.value
                    val timeString = timeInSeconds?.let {
                        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                        dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
                    }
                    inTime = timeString ?: "Na"
                } else {
                    val timeInSeconds = it.asDocument()["time"]?.asDateTime()?.value
                    val timeString = timeInSeconds?.let {
                        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
                        dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))
                    }
                    outTime = timeString ?: "Na"
                }
            }
            records+="\n${it.asDocument()["name"]?.asString()?.value},${it.asDocument()["rollNumber"]?.asString()?.value},$sem,$session,$inTime,$outTime"
        }
        val f = File(Uid.generate()+".csv").apply { writeText(records) }
        val resource = ByteArrayResource(f.readBytes())
        FileUtils.deleteFileAfterDelay(f, 5, TimeUnit.MINUTES)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students_${Uid.generate()}_$sem.csv")
            .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(f.length()).body(resource)

    }

    @GetMapping("/test")
    fun test(): String {
        return "{}"
    }
}