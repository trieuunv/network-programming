package org.np.model

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Users : LongIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val password = varchar("password", 64)
    val fullName = varchar("full_name", 100)
    val avatar = varchar("avatar", 500).nullable()
    val createDate = timestamp("create_date").clientDefault { Instant.now() }
}

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users)

    var username by Users.username
    var email by Users.email
    var password by Users.password
    var fullName by Users.fullName
    var avatar by Users.avatar
    var createDate by Users.createDate
}