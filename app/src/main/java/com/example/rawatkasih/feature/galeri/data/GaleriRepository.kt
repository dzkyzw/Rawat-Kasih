package com.example.rawatkasih.feature.galeri.data

import com.example.rawatkasih.feature.galeri.model.AlasanKoreksi
import com.example.rawatkasih.feature.galeri.model.FotoDraf
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.KategoriKeluhan
import com.example.rawatkasih.feature.galeri.model.Keparahan
import com.example.rawatkasih.feature.galeri.model.KoreksiDokumentasi
import com.example.rawatkasih.feature.galeri.model.Rekomendasi
import com.example.rawatkasih.feature.galeri.model.StatusTindak
import com.example.rawatkasih.feature.galeri.model.TandaBahaya
import com.example.rawatkasih.feature.galeri.model.TindakLanjut
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Fungsi 4 di atas Firestore (paket Spark, tanpa Cloud Storage):
 * - galeriFisik/{lansiaId}/foto/{id}: metadata + thumbnail JPEG Base64 untuk grid galeri.
 * - …/foto/{id}/gambar/{0..2}: foto penuh, dimuat hanya saat detail dibuka.
 * - …/foto/{id}/tindakLanjut/{id}: tindakan Selesai/Terjadwal.
 * - …/foto/{id}/koreksi/v{n}: riwayat perubahan berversi. Dokumentasi tidak pernah dihapus.
 */
internal class GaleriRepository(private val firestore: FirebaseFirestore) {
    private fun koleksi(lansiaId: String) = firestore.collection("galeriFisik").document(lansiaId).collection("foto")
    private fun dokumen(lansiaId: String, fotoId: String) = koleksi(lansiaId).document(fotoId)

    /** Dokumentasi seorang lansia sejak [sejak] (ms), terbaru di atas. */
    fun amatiGaleri(lansiaId: String, sejak: Long): Flow<List<FotoFisik>> = dengar(
        koleksi(lansiaId).whereGreaterThanOrEqualTo("waktu", Timestamp(Date(sejak)))
            .orderBy("waktu", Query.Direction.DESCENDING).limit(60)
    ) { bacaFoto(lansiaId, it) }

    /** Dokumentasi berstatus "Perlu dokter" yang belum ditangani, berapa pun umurnya (panel pengelola panti). */
    fun amatiPerluDokter(lansiaId: String): Flow<List<FotoFisik>> = dengar(
        koleksi(lansiaId).whereEqualTo("perluDokter", true).whereEqualTo("ditangani", false)
    ) { bacaFoto(lansiaId, it) }

    fun amatiTindakLanjut(lansiaId: String, fotoId: String): Flow<List<TindakLanjut>> = dengar(
        dokumen(lansiaId, fotoId).collection("tindakLanjut").orderBy("waktu", Query.Direction.DESCENDING)
    ) { d ->
        TindakLanjut(
            d.id, d.getString("tindakan")!!, StatusTindak.valueOf(d.getString("status")!!),
            waktu(d, "waktu")!!, d.getString("petugasEmail").orEmpty()
        )
    }

    fun amatiKoreksi(lansiaId: String, fotoId: String): Flow<List<KoreksiDokumentasi>> = dengar(
        dokumen(lansiaId, fotoId).collection("koreksi").orderBy("versi", Query.Direction.DESCENDING)
    ) { d ->
        KoreksiDokumentasi(
            d.id, d.getLong("versi")!!.toInt(), (d.get("perubahan") as? List<*>).orEmpty().filterIsInstance<String>(),
            AlasanKoreksi.valueOf(d.getString("alasan")!!), d.getString("olehEmail").orEmpty(), waktu(d, "waktu")!!
        )
    }

    suspend fun muatGambar(lansiaId: String, fotoId: String): List<String> =
        dokumen(lansiaId, fotoId).collection("gambar").orderBy("urutan").get().await().documents
            .mapNotNull { it.getString("data") }

    /** Menyimpan dokumentasi beserta semua fotonya dalam satu batch. Mengembalikan id dokumentasi. */
    suspend fun simpan(
        lansiaId: String,
        uid: String,
        email: String,
        foto: List<FotoDraf>,
        kategori: KategoriKeluhan,
        lokasi: String,
        observasi: String,
        tanda: Set<TandaBahaya>,
        rekomendasi: Rekomendasi,
        keparahan: Keparahan
    ): String {
        val dok = koleksi(lansiaId).document()
        val batch = firestore.batch()
        batch.set(
            dok,
            mapOf(
                "kategori" to kategori.name, "keparahan" to keparahan.name, "lokasi" to lokasi.trim(), "observasi" to observasi.trim(),
                "thumbnail" to foto.first().thumbnail, "jumlahFoto" to foto.size, "sopId" to (rekomendasi.sop?.id ?: ""),
                "tandaBahaya" to (tanda + rekomendasi.tandaTerdeteksi).map { it.name }.sorted(),
                "perluDokter" to rekomendasi.perluDokter, "alasanDokter" to rekomendasi.alasan, "ditangani" to false, "versi" to 1,
                "pengunggahUid" to uid, "pengunggahEmail" to email,
                "waktu" to FieldValue.serverTimestamp(), "diubahPada" to FieldValue.serverTimestamp()
            )
        )
        foto.forEachIndexed { i, f ->
            batch.set(dok.collection("gambar").document("$i"), mapOf("urutan" to i, "data" to f.penuh))
        }
        batch.commit().await()
        return dok.id
    }

    /** Tindak lanjut "Selesai" sekaligus menandai dokumentasi sudah ditangani. */
    suspend fun tambahTindakLanjut(foto: FotoFisik, tindakan: String, status: StatusTindak, waktu: Long?, uid: String, email: String) {
        val batch = firestore.batch()
        batch.set(
            dokumen(foto.lansiaId, foto.id).collection("tindakLanjut").document(),
            mapOf(
                "tindakan" to tindakan.trim(), "status" to status.name,
                // Selesai = waktu server saat disimpan; Terjadwal = jadwal yang dipilih.
                "waktu" to if (status == StatusTindak.SELESAI || waktu == null) FieldValue.serverTimestamp() else Timestamp(Date(waktu)),
                "petugasUid" to uid, "petugasEmail" to email, "dibuat" to FieldValue.serverTimestamp()
            )
        )
        if (status == StatusTindak.SELESAI && !foto.ditangani) {
            batch.update(dokumen(foto.lansiaId, foto.id), mapOf("ditangani" to true))
        }
        batch.commit().await()
    }

    /** Koreksi kategori/lokasi: versi naik satu dan riwayatnya tersimpan; foto dan versi asli tidak diubah. */
    suspend fun koreksi(
        foto: FotoFisik,
        kategori: KategoriKeluhan,
        lokasi: String,
        sopId: String?,
        perubahan: List<String>,
        alasan: AlasanKoreksi,
        uid: String,
        email: String
    ): Int {
        val versi = foto.versi + 1
        val batch = firestore.batch()
        batch.update(
            dokumen(foto.lansiaId, foto.id),
            mapOf(
                "kategori" to kategori.name, "lokasi" to lokasi.trim(), "sopId" to (sopId ?: ""),
                "versi" to versi, "diubahPada" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            dokumen(foto.lansiaId, foto.id).collection("koreksi").document("v$versi"),
            mapOf(
                "versi" to versi, "perubahan" to perubahan, "alasan" to alasan.name,
                "sebelum" to mapOf("kategori" to foto.kategori.name, "lokasi" to foto.lokasi),
                "olehUid" to uid, "olehEmail" to email, "waktu" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        return versi
    }

    private fun <T> dengar(query: Query, baca: (DocumentSnapshot) -> T?): Flow<List<T>> = callbackFlow {
        val r = query.addSnapshotListener { snap, error ->
            if (error != null) close(error) else if (snap != null) {
                trySend(snap.documents.mapNotNull { d -> runCatching { baca(d) }.getOrNull() })
            }
        }
        awaitClose { r.remove() }
    }

    private fun waktu(d: DocumentSnapshot, kolom: String) =
        d.getTimestamp(kolom, ServerTimestampBehavior.ESTIMATE)?.toDate()?.time

    private fun bacaFoto(lansiaId: String, d: DocumentSnapshot) = FotoFisik(
        id = d.id,
        lansiaId = lansiaId,
        kategori = KategoriKeluhan.valueOf(d.getString("kategori")!!),
        lokasi = d.getString("lokasi").orEmpty(),
        observasi = d.getString("observasi").orEmpty(),
        thumbnail = d.getString("thumbnail").orEmpty(),
        jumlahFoto = d.getLong("jumlahFoto")?.toInt() ?: 1,
        sopId = d.getString("sopId")?.ifEmpty { null },
        tandaBahaya = (d.get("tandaBahaya") as? List<*>).orEmpty()
            .mapNotNull { n -> TandaBahaya.entries.firstOrNull { it.name == n } }.toSet(),
        perluDokter = d.getBoolean("perluDokter") == true,
        alasanDokter = (d.get("alasanDokter") as? List<*>).orEmpty().filterIsInstance<String>(),
        ditangani = d.getBoolean("ditangani") == true,
        versi = d.getLong("versi")?.toInt() ?: 1,
        pengunggahId = d.getString("pengunggahUid")!!,
        pengunggahEmail = d.getString("pengunggahEmail").orEmpty(),
        waktu = waktu(d, "waktu")!!,
        keparahan = Keparahan.entries.firstOrNull { it.name == d.getString("keparahan") }
    )
}
