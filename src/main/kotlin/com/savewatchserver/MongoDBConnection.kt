package com.savewatchserver

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase


object MongoDBConnection {
    private const val CONNECTION_STRING = "mongodb://localhost:27017"
    private const val DATABASE_NAME = "safewatchserver"

    private val client: MongoClient by lazy {
        MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(CONNECTION_STRING))
                .build()
        )
    }

    val database: MongoDatabase by lazy {
        client.getDatabase(DATABASE_NAME)
    }

}
