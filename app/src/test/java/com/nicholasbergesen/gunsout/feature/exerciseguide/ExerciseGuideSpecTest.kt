package com.nicholasbergesen.gunsout.feature.exerciseguide

import com.nicholasbergesen.gunsout.data.entity.MovementPattern
import com.nicholasbergesen.gunsout.data.entity.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseGuideSpecTest {

    @Test fun `every muscle group produces visible target guidance`() {
        MuscleGroup.entries.forEach { muscle ->
            val spec = exerciseGuideSpecFor(muscle, MovementPattern.ISOLATION)

            assertTrue("$muscle label missing", spec.targetLabel.isNotBlank())
            assertTrue("$muscle cue missing", spec.targetCue.isNotBlank())
            assertTrue("$muscle highlight missing", spec.highlightedRegions.isNotEmpty())
        }
    }

    @Test fun `every movement pattern produces animated movement guidance`() {
        MovementPattern.entries.forEach { movement ->
            val spec = exerciseGuideSpecFor(MuscleGroup.CHEST, movement)

            assertTrue("$movement label missing", spec.movementLabel.isNotBlank())
            assertTrue("$movement cue missing", spec.movementCue.isNotBlank())
        }
    }

    @Test fun `full body and other use a broad fallback highlight`() {
        val fullBody = exerciseGuideSpecFor(MuscleGroup.FULL_BODY, MovementPattern.ISOLATION)
        val other = exerciseGuideSpecFor(MuscleGroup.OTHER, MovementPattern.ISOLATION)

        assertEquals(BodyMuscleRegion.entries.toSet(), fullBody.highlightedRegions)
        assertEquals(BodyMuscleRegion.entries.toSet(), other.highlightedRegions)
    }

    @Test fun `forearms use dedicated target region instead of broad fallback`() {
        val forearms = exerciseGuideSpecFor(MuscleGroup.FOREARMS, MovementPattern.ISOLATION)

        assertEquals("Forearms", forearms.targetLabel)
        assertEquals(setOf(BodyMuscleRegion.FOREARMS), forearms.highlightedRegions)
        assertFalse(BodyMuscleRegion.entries.toSet() == forearms.highlightedRegions)
        assertEquals(ExerciseGuideMotion.ISOLATION, forearms.motion)
    }

    @Test fun `adjacent arm and leg groups do not share target regions`() {
        val biceps = exerciseGuideSpecFor(MuscleGroup.BICEPS, MovementPattern.ISOLATION)
        val triceps = exerciseGuideSpecFor(MuscleGroup.TRICEPS, MovementPattern.ISOLATION)
        val quads = exerciseGuideSpecFor(MuscleGroup.QUADS, MovementPattern.SQUAT)
        val hamstrings = exerciseGuideSpecFor(MuscleGroup.HAMSTRINGS, MovementPattern.HINGE)

        assertFalse(biceps.highlightedRegions.contains(BodyMuscleRegion.TRICEPS))
        assertFalse(triceps.highlightedRegions.contains(BodyMuscleRegion.BICEPS))
        assertFalse(quads.highlightedRegions.contains(BodyMuscleRegion.HAMSTRINGS))
        assertFalse(hamstrings.highlightedRegions.contains(BodyMuscleRegion.QUADS))
    }

    @Test fun `movement patterns keep distinct animation motions`() {
        val push = exerciseGuideSpecFor(MuscleGroup.CHEST, MovementPattern.PUSH)
        val pull = exerciseGuideSpecFor(MuscleGroup.BACK, MovementPattern.PULL)
        val squat = exerciseGuideSpecFor(MuscleGroup.QUADS, MovementPattern.SQUAT)
        val hinge = exerciseGuideSpecFor(MuscleGroup.HAMSTRINGS, MovementPattern.HINGE)
        val lunge = exerciseGuideSpecFor(MuscleGroup.GLUTES, MovementPattern.LUNGE)
        val isolation = exerciseGuideSpecFor(MuscleGroup.BICEPS, MovementPattern.ISOLATION)
        val calves = exerciseGuideSpecFor(MuscleGroup.CALVES, MovementPattern.CALVES)
        val core = exerciseGuideSpecFor(MuscleGroup.CORE, MovementPattern.CORE)

        assertEquals(ExerciseGuideMotion.PUSH, push.motion)
        assertEquals(ExerciseGuideMotion.PULL, pull.motion)
        assertEquals(ExerciseGuideMotion.SQUAT, squat.motion)
        assertEquals(ExerciseGuideMotion.HINGE, hinge.motion)
        assertEquals(ExerciseGuideMotion.LUNGE, lunge.motion)
        assertEquals(ExerciseGuideMotion.ISOLATION, isolation.motion)
        assertEquals(ExerciseGuideMotion.CALVES, calves.motion)
        assertEquals(ExerciseGuideMotion.CORE, core.motion)
    }

    @Test fun `quad and hamstring isolation select leg-focused motion`() {
        val quads = exerciseGuideSpecFor(MuscleGroup.QUADS, MovementPattern.ISOLATION)
        val hamstrings = exerciseGuideSpecFor(MuscleGroup.HAMSTRINGS, MovementPattern.ISOLATION)

        assertEquals(ExerciseGuideMotion.LEG_ISOLATION, quads.motion)
        assertEquals(ExerciseGuideMotion.LEG_ISOLATION, hamstrings.motion)
    }

    @Test fun `calf and core isolation select their available motions`() {
        val calves = exerciseGuideSpecFor(MuscleGroup.CALVES, MovementPattern.ISOLATION)
        val core = exerciseGuideSpecFor(MuscleGroup.CORE, MovementPattern.ISOLATION)

        assertEquals(ExerciseGuideMotion.CALVES, calves.motion)
        assertEquals(ExerciseGuideMotion.CORE, core.motion)
    }

    @Test fun `upper-body isolation remains arm-focused`() {
        val biceps = exerciseGuideSpecFor(MuscleGroup.BICEPS, MovementPattern.ISOLATION)
        val triceps = exerciseGuideSpecFor(MuscleGroup.TRICEPS, MovementPattern.ISOLATION)
        val shoulders = exerciseGuideSpecFor(MuscleGroup.SHOULDERS, MovementPattern.ISOLATION)

        assertEquals(ExerciseGuideMotion.ISOLATION, biceps.motion)
        assertEquals(ExerciseGuideMotion.ISOLATION, triceps.motion)
        assertEquals(ExerciseGuideMotion.ISOLATION, shoulders.motion)
    }
}
