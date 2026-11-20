package com.example.rawatkasih.feature.obat.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.rawatkasih.data.local.RawatKasihDatabase
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ObatRepositoryTest {
    @Test
    fun stokDanRiwayatBertahanSetelahDatabaseDibukaUlang() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val namaDatabase = "uji-obat-${UUID.randomUUID()}.db"

        try {
            denganDatabase(context, namaDatabase) { repository ->
                assertEquals(4, repository.amatiRencana().first().stok)

                val pertama = async { repository.catat("08.00", StatusJadwal.DIMINUM) }
                val kedua = async { repository.catat("08.00", StatusJadwal.DIMINUM) }
                awaitAll(pertama, kedua)
                repository.catat("12.00", StatusJadwal.DILEWATI)
            }

            denganDatabase(context, namaDatabase) { repository ->
                val rencana = repository.amatiRencana().first()
                assertEquals(3, rencana.stok)
                assertTrue(rencana.stokMenipis)
                assertNull(rencana.jadwalBerikutnya)
                assertEquals(
                    listOf(
                        "12.00" to StatusJadwal.DILEWATI,
                        "08.00" to StatusJadwal.DIMINUM,
                        "06.00" to StatusJadwal.DIMINUM
                    ),
                    rencana.riwayat.map { it.jam to it.status }
                )
            }
        } finally {
            context.deleteDatabase(namaDatabase)
        }
        Unit
    }

    private suspend fun denganDatabase(
        context: Context,
        namaDatabase: String,
        uji: suspend (ObatRepository) -> Unit
    ) {
        val database = Room.databaseBuilder(context, RawatKasihDatabase::class.java, namaDatabase).build()
        try {
            uji(ObatRepository(database))
        } finally {
            database.close()
        }
    }
}
