package com.example.rawatkasih.ui.galeri

import com.example.rawatkasih.feature.galeri.model.BidangForm
import com.example.rawatkasih.feature.galeri.model.DrafDokumentasi
import com.example.rawatkasih.feature.galeri.model.FotoFisik
import com.example.rawatkasih.feature.galeri.model.KategoriKeluhan
import com.example.rawatkasih.feature.galeri.model.KoreksiDokumentasi
import com.example.rawatkasih.feature.galeri.model.Rekomendasi
import com.example.rawatkasih.feature.galeri.model.StatusDokumentasi
import com.example.rawatkasih.feature.galeri.model.TindakLanjut
import com.example.rawatkasih.feature.penugasan.model.Lansia

/** Pesan toast P3 (layar 50s, 52u, 52c); [fotoId] mengisi aksi "Lihat". */
internal data class ToastGaleri(val pesan: String, val fotoId: String? = null, val id: Long = System.nanoTime())

/** Aksi yang baru berhasil, dipakai layar untuk kembali ke tampilan sebelumnya. */
internal enum class AksiSelesai { DOKUMENTASI, TINDAK_LANJUT, KOREKSI }

internal data class DetailGaleri(
    val fotoId: String,
    val gambar: List<String>? = null,
    val gagalMuatGambar: Boolean = false,
    val tindakLanjut: List<TindakLanjut> = emptyList(),
    val koreksi: List<KoreksiDokumentasi> = emptyList()
)

internal data class GaleriUiState(
    val dimuat: Boolean = false,
    val gagalMemuat: Boolean = false,
    val peran: String? = null,
    val emailSaya: String = "",
    val lansia: List<Lansia> = emptyList(),
    /** Dokumentasi 4 minggu terakhir dari semua lansia dampingan, terbaru di atas. */
    val foto: List<FotoFisik> = emptyList(),
    val filter: KategoriKeluhan? = null,
    val draf: DrafDokumentasi = DrafDokumentasi(),
    val galat: Map<BidangForm, String> = emptyMap(),
    val pratinjau: Rekomendasi? = null,
    val sedangMemprosesFoto: Boolean = false,
    val menyimpan: Boolean = false,
    val pesanGagal: String? = null,
    val detail: DetailGaleri? = null,
    val toast: ToastGaleri? = null,
    val selesai: Pair<AksiSelesai, Long>? = null
) {
    /** Hanya pramurukti yang menambah dokumentasi, tindak lanjut, dan koreksi; keluarga melihat saja. */
    val bisaMengubah get() = peran == "PRAMURUKTI"
    val fotoTampil get() = foto.filter { filter == null || it.kategori == filter }
    fun namaLansia(id: String?) = lansia.firstOrNull { it.id == id }?.nama ?: "Lansia"
    fun cariFoto(id: String?) = foto.firstOrNull { it.id == id }
}

internal data class RingkasanPantau(val mingguIni: Int = 0, val lukaKulit: Int = 0, val memarBengkak: Int = 0)

internal data class PantauGaleriUiState(
    val dimuat: Boolean = false,
    val gagalMemuat: Boolean = false,
    val namaLansia: Map<String, String> = emptyMap(),
    val ringkasan: RingkasanPantau = RingkasanPantau(),
    val perluDokter: List<FotoFisik> = emptyList(),
    val terbaru: List<FotoFisik> = emptyList(),
    val menyimpan: Boolean = false,
    val pesan: String? = null
)

/** Nada pil untuk setiap status dokumentasi (Ditangani hijau, Baru/Dikoreksi ungu, Perlu dokter koral). */
internal val StatusDokumentasi.nada: P3Nada
    get() = when (this) {
        StatusDokumentasi.PERLU_DOKTER -> P3Nada.BAHAYA
        StatusDokumentasi.DIKOREKSI, StatusDokumentasi.BARU -> P3Nada.INFO
        StatusDokumentasi.DITANGANI -> P3Nada.OK
    }
