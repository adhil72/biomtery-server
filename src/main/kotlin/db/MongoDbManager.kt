package gecw.ace.db

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients

class MongoDbManager {
    companion object {
        lateinit var mongoClient: MongoClient

        fun connect() {
            mongoClient = MongoClients.create("mongodb+srv://adhilmhdk28:QVYtWKlypJGsp68q@cluster0.ce9y1h1.mongodb.net")
        }

    }
}
