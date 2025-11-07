package org.np.model

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Mails : LongIdTable("mails") {
    val sender = reference("sender_id", Users)
    val recipient = reference("recipient_id", Users)
    val subject = varchar("subject", 200)
    val content = text("content")
    val sentDate = timestamp("sent_date").clientDefault { Instant.now() }
    val isRead = bool("is_read").default(false)
}

class Mail(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Mail>(Mails)

    var sender by User referencedOn Mails.sender
    var recipient by User referencedOn Mails.recipient
    var subject by Mails.subject
    var content by Mails.content
    var sentDate by Mails.sentDate
    var isRead by Mails.isRead
}
