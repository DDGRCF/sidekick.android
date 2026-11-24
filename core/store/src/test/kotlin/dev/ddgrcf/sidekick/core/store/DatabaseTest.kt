package dev.ddgrcf.sidekick.core.store

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {
    private lateinit var db: SidekickDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SidekickDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun messagesResetClearsConversationAtomically() = runTest {
        val dao = db.messageDao()
        dao.upsertAll(
            listOf(
                MessageEntity("b1", "c1", 1, "USER", "[]", 100L, null),
                MessageEntity("b1", "c1", 2, "ASSISTANT", "[]", 200L, "gpt-5"),
                MessageEntity("b1", "c2", 1, "USER", "[]", 300L, null),
            ),
        )
        assertEquals(2L, dao.maxSeq("b1", "c1"))
        dao.resetMessages("b1", "c1")
        assertEquals(0, dao.observeMessages("b1", "c1").first().size)
        assertEquals(1, dao.observeMessages("b1", "c2").first().size)
    }
}
