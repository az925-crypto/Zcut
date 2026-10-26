package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.Timeline
import java.util.UUID

/**
 * Re-identifies a draft that came back from a local backup (F20).
 *
 * Asset ids and the project id are regenerated so importing the same backup
 * twice cannot make two drafts share media ids (thumbnails, clip lookups and
 * `MediaAppender` all key on them). Paths are only rewritten for assets whose
 * media file was actually unpacked; a missing file keeps its old path instead of
 * silently pointing every clip at a deleted file.
 */
object DraftRemap {

    const val MAX_NAME_LENGTH = 80
    const val IMPORT_SUFFIX = " (impor)"

    fun imported(
        source: Project,
        newPaths: Map<String, String>,
        idFor: (String) -> String = { UUID.randomUUID().toString() },
        nowMs: Long = System.currentTimeMillis(),
        name: String = importedName(source.name)
    ): Project {
        val ids = source.assets.associate { it.id to idFor(it.id) }
        val assets = source.assets.map { asset ->
            asset.copy(
                id = ids[asset.id] ?: asset.id,
                uri = "",
                localPath = newPaths[asset.id] ?: asset.localPath
            )
        }
        return source.copy(
            id = idFor(source.id),
            name = name,
            createdAtMs = nowMs,
            updatedAtMs = nowMs,
            trashedAtMs = null,
            assets = assets,
            coverAssetId = source.coverAssetId?.let { ids[it] ?: it },
            timeline = remapTimeline(source.timeline, ids)
        )
    }

    fun importedName(name: String): String {
        val base = name.trim().ifBlank { "Draft" }
        if (base.length + IMPORT_SUFFIX.length <= MAX_NAME_LENGTH) {
            return base + IMPORT_SUFFIX
        }
        return base.take(MAX_NAME_LENGTH - IMPORT_SUFFIX.length).trimEnd() + IMPORT_SUFFIX
    }

    fun remapTimeline(timeline: Timeline, ids: Map<String, String>): Timeline =
        timeline.copy(
            tracks = timeline.tracks.map { track ->
                track.copy(
                    clips = track.clips.map { clip ->
                        val assetId = clip.assetId
                        val mapped = assetId?.let(ids::get)
                        if (assetId != null && mapped != null) {
                            clip.copy(assetId = mapped)
                        } else {
                            clip
                        }
                    }
                )
            }
        )
}
