# 20 — Draft Management, Cloud & Collaboration

> **Zcut policy:** fitur ini gratis, tanpa paywall/watermark, dan tidak memanggil server aplikasi asli.  
> Draft lokal selalu tersedia; backup/kolaborasi ditandai **BUTUH BACKEND**.

## Bukti

- Client: `DraftClient`, `CloudDraftClient`, `StdraftClient`, `MiniDraftClient`,
  `SubDraftClient`, `DraftTransformClient`, `DraftInfoExtractionClient`.
- Package `com.vega.cloud`: `autobackup` (68 view + 53 setting),
  `upload/view` (125), `sharedraft` (81 utils + 37 model),
  `review` (71 fragment + 58 model), `group/view` (85), `brand/view` (84),
  `enterprise/view` (95), `download/material` (49).
- Room: `DraftDatabase` v2, `Migration1To2`, `StableEntity`,
  `MediaDataTransEntity`.
- Modul native: `DraftBeginTransaction`, `DraftCommit`, `DraftAmend`,
  `DraftDeepCopy`, `DraftCanUndo/Redo`,
  `IDraftUpdateCallback`/`IDraftNodeUpdateCallback`.
- `Patch`/`KvStorage`/`MultiProcess` module — kemungkinan draft patch & state
  lintas proses.
- Endpoint: `feed-api.capcutapi.com`, `editor-api.capcutapi.com`,
  `mon-v2-boot.capcutapi.com`.

## Bentuk penyimpanan

| Lapis | Isi |
|---|---|
| Draft files | `draft_content.json`, `draft_meta_info.json`, folder material |
| Mini draft | paket ringkas untuk preview/kolaborasi (`minidraft` header) |
| Room | metadata stabil, riwayat transcode, index draft |
| KV | preferensi, state UI, session |
| Cloud | backup, shared draft, review, brand, enterprise |

Draft adalah **local-first**. Cloud bersifat eventual: upload di background,
konflik diselesaikan dengan timestamp/versi.

## Operasi

- **Autosave**: debounce setelah edit, tulis atomic (temp + rename).
- **Backup**: upload draft + material ke cloud; restore/download.
- **Shared draft**: link + izin; kolaborator membuka mini draft.
- **Review**: komentar per timestamp/frame, approval.
- **Group/team**: workspace, anggota, role.
- **Brand kit**: logo, warna, font, intro/outro untuk enterprise.
- **Enterprise**: kontrol admin, audit, template terkunci.
- **Trash/restore**: soft delete + pembersihan material orphan.

## Alur sync

```
edit lokal
  → autosave (debounce 500–1500 ms)
  → hitung versi + hash
  → queue upload (draft json + material baru)
  → server simpan versi
  → device lain pull → merge
```

Konflik:

```kotlin
enum class ConflictPolicy { LOCAL_WINS, REMOTE_WINS, NEWEST_WINS, MANUAL }
```

Untuk draft JSON, merge otomatis berisiko; CapCut tampaknya memilih versi
(memakai `update_time`/version), bukan three-way merge.

## Implementasi referensi (clean-room)

```kotlin
class DraftSyncEngine(
    private val local: DraftStore,
    private val remote: CloudApi,
    private val connectivity: Connectivity,
) {
    suspend fun sync(draftId: String): SyncResult {
        val localMeta = local.meta(draftId)
        val remoteMeta = remote.meta(draftId)

        val winner = when {
            remoteMeta == null -> localMeta
            localMeta.updatedAt >= remoteMeta.updatedAt -> localMeta
            else -> remoteMeta
        }

        if (winner === remoteMeta) {
            val bundle = remote.download(remoteMeta.version)
            local.install(bundle)              // atomic replace
            return SyncResult.Pulled(remoteMeta.version)
        }

        if (localMeta.version != remoteMeta?.version) {
            remote.upload(local.snapshot(draftId))   // conflict: local newer
        }
        return SyncResult.Pushed(localMeta.version)
    }
}
```

Upload material chunk:

```kotlin
suspend fun uploadMaterial(file: File, chunk: Int = 4 * 1024 * 1024) {
    val uploadId = api.initUpload(file.length(), file.md5())
    var offset = 0L
    file.inputStream().use { input ->
        val buf = ByteArray(chunk)
        while (true) {
            val n = input.read(buf)
            if (n <= 0) break
            api.putChunk(uploadId, offset, buf.copyOf(n))
            offset += n
        }
    }
    api.completeUpload(uploadId)
}
```

## Catatan paritas

- **P0**: autosave atomic, list/rename/delete/duplicate, restore.
- **P1**: backup cloud, shared draft read-only, export/import project.
- **P2**: kolaborasi realtime, review, brand kit, enterprise, version history.

## Unknown

- Format mini draft dan protokol patch tidak terbaca.
- Apakah kolaborasi memakai OT/CRDT atau lock; tidak ada bukti CRDT di nama
  class.
- Kuota cloud dan retensi tidak terbaca.
