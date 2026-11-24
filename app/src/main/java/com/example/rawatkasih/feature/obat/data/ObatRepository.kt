package com.example.rawatkasih.feature.obat.data

import androidx.room.withTransaction
import com.example.rawatkasih.data.local.RawatKasihDatabase
import com.example.rawatkasih.feature.obat.data.local.JadwalObatEntity
import com.example.rawatkasih.feature.obat.data.local.RencanaObatDenganJadwal
import com.example.rawatkasih.feature.obat.data.local.RencanaObatEntity
import com.example.rawatkasih.feature.obat.model.JadwalObat
import com.example.rawatkasih.feature.obat.model.RencanaObat
import com.example.rawatkasih.feature.obat.model.StatusJadwal
import com.example.rawatkasih.feature.obat.model.rencanaObatContoh
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class ObatRepository(private val database: RawatKasihDatabase) : SumberObat {
    private val dao = database.obatDao()

    override fun amatiRencana() = flow {
        database.withTransaction {
            if (dao.ambilRencana(ID_RENCANA_CONTOH) == null) {
                val contoh = rencanaObatContoh()
                dao.tambahRencana(
                    RencanaObatEntity(ID_RENCANA_CONTOH, contoh.namaLansia, contoh.stok, contoh.batasStok)
                )
                dao.tambahJadwal(contoh.jadwal.map {
                    JadwalObatEntity(ID_RENCANA_CONTOH, it.jam, it.waktu, it.status)
                })
            }
        }
        emitAll(dao.amatiRencana(ID_RENCANA_CONTOH).map { checkNotNull(it).keModel() })
    }

    override suspend fun catat(jam: String, status: StatusJadwal) = database.withTransaction {
        val lama = checkNotNull(dao.ambilRencana(ID_RENCANA_CONTOH)).keModel()
        val baru = lama.catat(jam, status)
        if (baru == lama) return@withTransaction

        dao.ubahStatus(ID_RENCANA_CONTOH, jam, status)
        if (baru.stok != lama.stok) dao.ubahStok(ID_RENCANA_CONTOH, baru.stok)
    }

    private fun RencanaObatDenganJadwal.keModel() = RencanaObat(
        namaLansia = rencana.namaLansia,
        stok = rencana.stok,
        batasStok = rencana.batasStok,
        jadwal = jadwal.sortedBy { it.jam }.map { JadwalObat(it.jam, it.waktu, it.status) }
    )

    private companion object {
        const val ID_RENCANA_CONTOH = 1
    }
}
