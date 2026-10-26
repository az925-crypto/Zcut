package com.zaaam.zcut.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentPlannerTest {

    private fun tools(prompt: String): List<AgentPlanner.Tool> =
        AgentPlanner.plan(prompt).steps.map { it.tool }

    @Test
    fun theGreetingAndSuggestionsAreTheMockupWording() {
        assertEquals(
            "What are we making? I can build a rough cut, add captions, or find the " +
                "best 15 seconds.",
            AgentPlanner.GREETING
        )
        assertEquals(
            listOf("Tighten the intro", "Add captions", "Find a hook"),
            AgentPlanner.SUGGESTIONS
        )
    }

    @Test
    fun aTightenRequestRoutesToTheLocalTrim() {
        val plan = AgentPlanner.plan("Please tighten the intro")

        assertEquals(listOf(AgentPlanner.Tool.TIGHTEN_INTRO), plan.steps.map { it.tool })
        assertTrue(plan.steps.single().runnable)
        assertTrue(plan.applies)
    }

    @Test
    fun aMusicRequestRoutesToTheLocalRender() {
        val plan = AgentPlanner.plan("add a soft beat")

        assertEquals(listOf(AgentPlanner.Tool.ADD_MUSIC), plan.steps.map { it.tool })
        assertTrue(plan.applies)
    }

    @Test
    fun aHighlightRequestPointsAtTheClipperWithoutPretendingToRun() {
        val plan = AgentPlanner.plan("find a hook")

        val step = plan.steps.single()
        assertEquals(AgentPlanner.Tool.AUTO_CUT, step.tool)
        assertFalse(step.runnable)
        assertEquals(AgentPlanner.ROUTE_CLIPPER, step.route)
        assertFalse("nothing runs locally", plan.applies)
    }

    @Test
    fun aCaptionRequestIsMarkedAsNeedingABackend() {
        val plan = AgentPlanner.plan("add captions")

        val step = plan.steps.single()
        assertEquals(AgentPlanner.Tool.ADD_CAPTION, step.tool)
        assertFalse(step.runnable)
        assertNull(step.route)
        assertFalse(plan.applies)
    }

    @Test
    fun responsesMentionBothLocalAndBackendSteps() {
        val mixed = AgentPlanner.plan("tighten the intro and find a hook")

        assertEquals(
            listOf(AgentPlanner.Tool.TIGHTEN_INTRO, AgentPlanner.Tool.AUTO_CUT),
            mixed.steps.map { it.tool }
        )
        assertEquals(1, mixed.runnableSteps.size)
        assertTrue(mixed.applies)
        assertTrue(mixed.reply.contains("1 local tool call(s) ready"))
        assertTrue(mixed.reply.contains("1 step(s) need a backend"))
    }

    @Test
    fun everyRecognisedStepRunsHere() {
        val plan = AgentPlanner.plan("tighten the intro and add music")

        assertEquals(2, plan.runnableSteps.size)
        assertEquals(plan.steps, plan.runnableSteps)
        assertTrue(
            plan.reply.contains(
                "2 local tool call(s) ready. Review them before I touch the timeline."
            )
        )
    }

    @Test
    fun anUnrecognisedPromptFallsBackToTheLocalToolsAndSaysSo() {
        val plan = AgentPlanner.plan("make it pop somehow")

        assertEquals(
            listOf(AgentPlanner.Tool.TIGHTEN_INTRO, AgentPlanner.Tool.ADD_MUSIC),
            plan.steps.map { it.tool }
        )
        assertTrue(plan.applies)
        assertTrue(plan.reply.startsWith("I did not catch a specific request"))
    }

    @Test
    fun routingIsCaseInsensitive() {
        assertEquals(listOf(AgentPlanner.Tool.ADD_CAPTION), tools("ADD SUBTITLES"))
        assertEquals(listOf(AgentPlanner.Tool.AUTO_CUT), tools("Best moment please"))
    }

    @Test
    fun aBackendOnlyPlanDeclaresNothingIsRunnable() {
        val plan = AgentPlanner.plan("add captions")

        assertTrue(plan.runnableSteps.isEmpty())
        assertTrue(plan.reply.contains("Nothing here can run on device yet."))
    }
}