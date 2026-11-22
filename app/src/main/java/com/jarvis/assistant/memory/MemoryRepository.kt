package com.jarvis.assistant.memory

import com.jarvis.assistant.conversation.MemoryFact
import com.jarvis.assistant.conversation.MemoryFactDao
import com.jarvis.assistant.conversation.MemoryFactEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface MemoryRepository {
    fun observeFacts(): Flow<List<MemoryFact>>
    suspend fun getFacts(): List<MemoryFact>
    suspend fun addFact(text: String): Long
    suspend fun deleteFact(id: Long)
    suspend fun clear()
}

class RoomMemoryRepository(
    private val dao: MemoryFactDao,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : MemoryRepository {
    override fun observeFacts(): Flow<List<MemoryFact>> =
        dao.observeFacts().map { list -> list.map { MemoryFact(it.id, it.text, it.createdAt) } }

    override suspend fun getFacts(): List<MemoryFact> =
        dao.getFacts().map { MemoryFact(it.id, it.text, it.createdAt) }

    override suspend fun addFact(text: String): Long =
        dao.insertFact(MemoryFactEntity(text = text.trim(), createdAt = clock()))

    override suspend fun deleteFact(id: Long) = dao.deleteFact(id)

    override suspend fun clear() = dao.clearFacts()
}
