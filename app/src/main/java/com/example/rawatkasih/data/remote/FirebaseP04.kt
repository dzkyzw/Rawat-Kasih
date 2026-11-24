package com.example.rawatkasih.data.remote

import android.content.Context
import com.example.rawatkasih.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings

internal class FirebaseP04 private constructor(
    val auth: FirebaseAuth,
    val firestore: FirebaseFirestore,
    val authPendaftaran: FirebaseAuth
) {
    companion object {
        private var instance: FirebaseP04? = null

        @Synchronized
        fun ambil(context: Context): FirebaseP04? {
            instance?.let { return it }
            val appContext = context.applicationContext
            val app = if (BuildConfig.FIREBASE_EMULATOR) {
                FirebaseApp.initializeApp(
                    appContext,
                    FirebaseOptions.Builder()
                        .setProjectId("demo-rawat-kasih")
                        .setApplicationId("1:1234567890:android:0000000000000000")
                        .setApiKey("demo-api-key")
                        .build(),
                    "p04-emulator"
                )
            } else {
                FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                    ?: return null
            }
            val auth = FirebaseAuth.getInstance(app)
            // Account creation signs in on this instance, leaving the admin's session intact.
            val appPendaftaran = FirebaseApp.initializeApp(appContext, app.options, "${app.name}-pendaftaran")
            val authPendaftaran = FirebaseAuth.getInstance(appPendaftaran)
            val firestore = FirebaseFirestore.getInstance(app)
            firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
            if (BuildConfig.FIREBASE_EMULATOR) {
                auth.useEmulator("10.0.2.2", 9211)
                authPendaftaran.useEmulator("10.0.2.2", 9211)
                firestore.useEmulator("10.0.2.2", 8211)
            }
            authPendaftaran.signOut()
            return FirebaseP04(auth, firestore, authPendaftaran).also { instance = it }
        }
    }
}
internal fun idsPenugasan(daftar: Any?, lama: String?): List<String> =
    ((daftar as? List<*>)?.filterIsInstance<String>() ?: listOfNotNull(lama)).distinct()
