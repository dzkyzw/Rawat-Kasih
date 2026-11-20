package com.example.rawatkasih.feature.obat.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.rawatkasih.feature.obat.model.StatusJadwal

@Entity(tableName = "rencana_obat")
internal data class RencanaObatEntity(
    @PrimaryKey val id: Int,
    val namaLansia: String,
    val stok: Int,
    val batasStok: Int
)

@Entity(
    tableName = "jadwal_obat",
    primaryKeys = ["rencanaId", "jam"],
    foreignKeys = [
        ForeignKey(
            entity = RencanaObatEntity::class,
            parentColumns = ["id"],
            childColumns = ["rencanaId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
internal data class JadwalObatEntity(
    val rencanaId: Int,
    val jam: String,
    val waktu: String,
    val status: StatusJadwal
)

internal data class RencanaObatDenganJadwal(
    @Embedded val rencana: RencanaObatEntity,
    @Relation(parentColumn = "id", entityColumn = "rencanaId")
    val jadwal: List<JadwalObatEntity>
)
