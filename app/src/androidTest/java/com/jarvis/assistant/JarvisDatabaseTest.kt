package com.jarvis.assistant

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jarvis.assistant.conversation.JarvisDatabase
import com.jarvis.assistant.conversation.RoomConversationRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JarvisDatabaseTest {

    private lateinit var db: JarvisDatabase
    private lateinit var repo: RoomConversationRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            JarvisDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = RoomConversationRepository(db.conversationDao(), db.messageDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertsAndReadsConversationMessages() = runBlocking {
        val convId = repo.createConversation("Test Thread")
        repo.addMessage(convId, "user", "Vanakkam", "user")
        repo.addMessage(convId, "assistant", "Vanakkam! How can I help?", "ai")

        val msgs = repo.getMessages(convId)
        assertEquals(2, msgs.size)
        assertEquals("Vanakkam", msgs[0].content)
    }
}
