import { parseArgs } from "node:util";
import { applicationDefault, deleteApp, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore } from "firebase-admin/firestore";

const { values } = parseArgs({
  options: {
    emulator: { type: "boolean" },
    project: { type: "string" },
    pramurukti: { type: "string" },
    keluarga: { type: "string" },
    admin: { type: "string" },
  },
});
const demoProject = "demo-rawat-kasih";
const emulator = values.emulator === true;
const projectId = emulator ? demoProject : values.project;

if (emulator && (values.project || values.pramurukti || values.keluarga || values.admin)) {
  throw new Error("--emulator memakai proyek demo dan akun lokal sendiri.");
}
if (emulator) {
  process.env.FIREBASE_AUTH_EMULATOR_HOST ??= "127.0.0.1:9211";
  process.env.FIRESTORE_EMULATOR_HOST ??= "127.0.0.1:8211";
  if (![process.env.FIREBASE_AUTH_EMULATOR_HOST, process.env.FIRESTORE_EMULATOR_HOST]
    .every((host) => /^(127[.]0[.]0[.]1|localhost):[0-9]+$/.test(host))) {
    throw new Error("Host emulator harus berada di komputer ini.");
  }
} else if (!projectId || !(values.admin || (values.pramurukti && values.keluarga)) ||
    projectId.startsWith("demo-") ||
    process.env.FIREBASE_AUTH_EMULATOR_HOST || process.env.FIRESTORE_EMULATOR_HOST) {
  throw new Error("Gunakan --project PROJECT_ID --admin UID atau --pramurukti UID_A --keluarga UID_B.");
}

const app = initializeApp(emulator ? { projectId } : { projectId, credential: applicationDefault() });
const auth = getAuth(app);
const db = getFirestore(app);

async function akunEmulator(email) {
  try {
    return await auth.getUserByEmail(email);
  } catch (error) {
    if (error.code !== "auth/user-not-found") throw error;
    return await auth.createUser({ email, password: "RawatKasih-Demo-Lokal", emailVerified: true });
  }
}

try {
  const peranDemo = [
    ["PRAMURUKTI", values.pramurukti, "pramurukti@rawatkasih.test"],
    ["KELUARGA", values.keluarga, "keluarga@rawatkasih.test"],
    ["ADMIN", values.admin, "admin@rawatkasih.test"],
  ];
  const uidCloud = peranDemo.map(([, uid]) => uid).filter(Boolean);
  if (new Set(uidCloud).size !== uidCloud.length) {
    throw new Error("Gunakan UID berbeda untuk setiap peran.");
  }
  const akun = await Promise.all(peranDemo.filter(([, uid]) => emulator || uid)
    .map(async ([peran, uid, email]) => ({
      peran,
      pengguna: emulator ? await akunEmulator(email) : await auth.getUser(uid),
    })));
  if (akun.some(({ pengguna }) => !pengguna.providerData.some(({ providerId }) => providerId === "password"))) {
    throw new Error("Akun demo harus tersedia di Firebase Authentication dengan login Email/Password.");
  }
  const pencatat = akun.find(({ peran }) => peran === "PRAMURUKTI")?.pengguna.uid;

  const rencana = db.collection("rencanaObat").doc("oma-ratih-demo");
  const rencanaKedua = db.collection("rencanaObat").doc("opa-damar-demo");
  const katalog = [
    [db.collection("lansiaP04").doc(rencana.id), "Oma Ratih"],
    [db.collection("lansiaP04").doc(rencanaKedua.id), "Opa Damar"],
  ];
  const akses = akun.map(({ peran, pengguna }) => [
    db.collection("aksesObat").doc(pengguna.uid),
    peran === "ADMIN" ? { peran } : { peran, rencanaId: rencana.id },
  ]);
  await db.runTransaction(async (tx) => {
    const [data, dataKedua, katalogPertama, katalogKedua, ...daftarAkses] = await tx.getAll(
      rencana, rencanaKedua, ...katalog.map(([ref]) => ref), ...akses.map(([ref]) => ref)
    );
    for (const [index, snapshot] of daftarAkses.entries()) {
      const target = akses[index][1];
      if (snapshot.exists &&
          (snapshot.get("rencanaId") !== target.rencanaId || snapshot.get("peran") !== target.peran)) {
        throw new Error("Salah satu akun sudah punya akses lain. Gunakan akun demo yang berbeda.");
      }
    }
    if (!data.exists) {
      tx.create(rencana, {
        namaLansia: "Oma Ratih",
        stok: 5,
        batasStok: 4,
        aksi: { jenis: "AWAL", jam: "" },
        jadwal: {
          "07.00": {
            waktu: "Pagi",
            status: "DIMINUM",
            ...(pencatat ? { dicatatOleh: pencatat } : {}),
            dicatatPada: FieldValue.serverTimestamp(),
          },
          "09.00": { waktu: "Pagi", status: "BELUM_DIKONFIRMASI" },
          "13.00": { waktu: "Siang", status: "BELUM_DIKONFIRMASI" },
        },
      });
    }
    if (!dataKedua.exists) {
      tx.create(rencanaKedua, {
        namaLansia: "Opa Damar",
        stok: 6,
        batasStok: 2,
        aksi: { jenis: "AWAL", jam: "" },
        jadwal: { "10.00": { waktu: "Pagi", status: "BELUM_DIKONFIRMASI" } },
      });
    }
    for (const [index, snapshot] of [katalogPertama, katalogKedua].entries()) {
      const [ref, namaLansia] = katalog[index];
      if (!snapshot.exists) tx.create(ref, { namaLansia });
    }
    for (const [index, [ref, isi]] of akses.entries()) {
      if (!daftarAkses[index].exists) tx.create(ref, isi);
    }
  });
  console.log(`Data demo P04 tersedia di proyek ${projectId}.`);
} finally {
  await deleteApp(app);
}
