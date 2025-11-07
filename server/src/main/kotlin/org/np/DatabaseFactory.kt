package org.np

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.np.model.Mails
import org.np.model.Users

object DatabaseFactory {
    fun init() {
        Database.connect(
            url = "jdbc:mysql://localhost:3306/network_exposed?useSSL=false&serverTimezone=UTC",
            driver = "com.mysql.cj.jdbc.Driver",
            user = "root",
            password = ""
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Users, Mails)
        }
    }
}