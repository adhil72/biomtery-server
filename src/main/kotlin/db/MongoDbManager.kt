package gecw.ace.db

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients

class MongoDbManager {
    companion object {
        lateinit var mongoClient: MongoClient

        fun connect() {
            mongoClient = MongoClients.create("mongodb://localhost:27017")
        }

    }
}
