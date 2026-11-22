package com.jarvis.assistant.device

import com.jarvis.assistant.conversation.Reminder
import com.jarvis.assistant.conversation.ReminderDao
import com.jarvis.assistant.conversation.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ReminderRepository {
    suspend fun add(text: String, triggerAt: Long): Long
    fun observeAll(): Flow<List<Reminder>>
    suspend fun pending(now: Long): List<Reminder>
    suspend fun markFired(id: Long)
    suspend fun delete(id: Long)
    suspend fun clear()
}

class RoomReminderRepository(
    private val dao: ReminderDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ReminderRepository {
    override suspend fun add(text: String, triggerAt: Long): Long =
        dao.insert(ReminderEntity(text = text.trim(), triggerAt = triggerAt, fired = false, createdAt = clock()))

    override fun observeAll(): Flow<List<Reminder>> =
        dao.observeAll().map { list -> list.map { Reminder(it.id, it.text, it.triggerAt, it.fired, it.createdAt) } }

    override suspend fun pending(now: Long): List<Reminder> =
        dao.pending(now).map { Reminder(it.id, it.text, it.triggerAt, it.fired, it.createdAt) }

    override suspend fun markFired(id: Long) = dao.markFired(id)
    override suspend fun delete(id: Long) = dao.delete(id)
    override suspend fun clear() = dao.clear()
}
