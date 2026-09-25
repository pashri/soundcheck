package org.pashri.soundcheck.warmup

/**
 * The Iteration playing at [frame].
 *
 * @param frame a position in frames from the start of the Step.
 * @return the Iteration whose span holds [frame], or null during the Announcement, gap or
 *     Demo, or after the Step ends.
 */
fun StepTimeline.iterationAt(frame: Long): IterationSpan? =
    iterations.firstOrNull { frame >= it.startFrame && frame < it.endFrame }

/**
 * Where to resume after pausing at [pausedAtFrame]. Pausing abandons the current Iteration
 * and resuming replays it from its Key Chord.
 *
 * @param pausedAtFrame the position of the pause, in frames from the start of the Step.
 * @return the paused Iteration's first frame; 0 if the pause came before the first
 *     Iteration, so the whole Step replays; [StepTimeline.lengthFrames] if the Step had
 *     already ended.
 */
fun StepTimeline.resumeFrame(pausedAtFrame: Long): Long =
    if (pausedAtFrame >= lengthFrames) {
        lengthFrames
    } else {
        iterationAt(pausedAtFrame)?.startFrame ?: 0L
    }

/**
 * The events to schedule when playing from [frame] onward.
 *
 * @param frame a position in frames from the start of the Step, usually a [resumeFrame].
 * @return every event that starts at or after [frame], in order.
 */
fun StepTimeline.eventsFrom(frame: Long): List<TimelineEvent> =
    eventsBetween(from = frame, until = Long.MAX_VALUE)

/**
 * The events that start inside a window of the Step, for scheduling a little at a time.
 *
 * @param from the window's first frame, counted from the start of the Step.
 * @param until the first frame after the window.
 * @return every event with [from] ≤ start < [until], in order.
 */
fun StepTimeline.eventsBetween(from: Long, until: Long): List<TimelineEvent> =
    events.filter { it.startFrame >= from && it.startFrame < until }
