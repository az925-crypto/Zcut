package com.zaaam.zcut.core.domain

import com.zaaam.zcut.core.model.Project
import com.zaaam.zcut.core.model.TrackType

/**
 * The one timeline edit the local agent can perform by itself.
 *
 * `AgentPlanner` decides *what* to do; this is *how* it is done, so the change
 * is a pure function that a JVM test can pin. Music is added by the ViewModel
 * because it renders a file first.
 */
object AgentEditor {

    /** Matches the agent's own "0.8s" wording. */
    const val INTRO_HEAD_MS = 800L

    /**
     * Trims [headMs] off the head of the earliest clip on the video track and
     * ripples the rest of the timeline left by the same amount, so no gap is
     * left behind and the overlay/text/audio tracks stay in sync with the cut.
     */
    fun tightenIntro(project: Project, headMs: Long = INTRO_HEAD_MS): Project {
        val videoTrack = project.timeline.tracks
            .firstOrNull { it.type == TrackType.VIDEO }
            ?: return project
        val first = videoTrack.clips.minByOrNull { it.startMs } ?: return project
        val head = headMs.coerceAtMost((first.durationMs - 1L).coerceAtLeast(0L))
        if (head <= 0L) return project

        val trimmed = TimelineEditor.trimClip(
            project.timeline,
            first.id,
            first.startMs + head,
            first.endMs
        )
        if (trimmed == project.timeline) return project

        val anchor = first.startMs
        val shifted = trimmed.copy(
            tracks = trimmed.tracks.map { track ->
                track.copy(
                    clips = track.clips.map { clip ->
                        if (clip.startMs >= anchor) {
                            clip.copy(startMs = (clip.startMs - head).coerceAtLeast(0L))
                        } else {
                            clip
                        }
                    }
                )
            }
        )
        return project.copy(timeline = TimelineEditor.recalculate(shifted))
    }
}