package org.pashri.soundcheck.warmup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.pashri.soundcheck.audio.SoundOutput
import org.pashri.soundcheck.audio.msToFrames
import org.pashri.soundcheck.piano.Piano

/**
 * Where a Programme has got to.
 *
 * @property programme the Programme.
 * @property range the Range it plays through.
 * @property stepIndex the Step sounding now; while paused, the Step that resumes.
 * @property iteration the Iteration sounding (or resuming), from 0; null during the
 *     Announcement, the gap, the Demo and the pause between Steps.
 * @property playing false while paused.
 */
data class Playback(
    val programme: Programme,
    val range: Range,
    val stepIndex: Int,
    val iteration: Int?,
    val playing: Boolean,
)

/**
 * Plays a Programme through a [SoundOutput]: each Step's Announcement, Demo and Iterations on
 * exact frames, Step after Step with a gap between them, skipping Steps that don't fit.
 *
 * Sounds are handed to the engine [LOOKAHEAD_MS] ahead and topped up every [TICK_MS], so the
 * engine never holds more than a second of them. Call every method from the thread [scope]
 * runs on.
 *
 * @param output where the sounds play.
 * @param piano the piano the notes play on.
 * @param announcements each Sound's Announcement.
 * @param scope runs the scheduling loop.
 */
class ProgrammePlayer(
    private val output: SoundOutput,
    private val piano: Piano,
    private val announcements: Announcements,
    private val scope: CoroutineScope,
) {
    private val _playback = MutableStateFlow<Playback?>(null)

    /** The Programme playing or paused, or null when stopped. */
    val playback: StateFlow<Playback?> = _playback.asStateFlow()

    private var loop: Job? = null
    private var tail: Job? = null
    private var preparing: Job? = null
    private var resumeAt: ResumePoint? = null
    private var startedAt: ResumePoint? = null
    private val segments = ArrayDeque<Segment>()

    /**
     * Plays [programme] from its first Step that fits, replacing anything playing.
     *
     * @param programme the Programme.
     * @param range the Range it plays through.
     * @return false if no Step fits (nothing plays), or if the output won't start (it stays
     *     paused on the first Step, so [resume] tries again).
     */
    fun play(programme: Programme, range: Range): Boolean {
        val first = programme.firstStep(range) ?: return false
        preparing?.cancel()
        preparing = scope.launch { prepareAnnouncements(programme = programme, range = range) }
        val start = Playback(
            programme = programme,
            range = range,
            stepIndex = first,
            iteration = null,
            playing = true,
        )
        return startAt(base = start, point = ResumePoint(step = first, frame = 0L))
    }

    /**
     * Pauses, abandoning the Iteration that is sounding.
     *
     * @param fade true to fade out and release the device output a moment later (the user
     *     paused); false to silence and release it at once, because another tool is about
     *     to start.
     */
    fun pause(fade: Boolean = true) {
        val current = _playback.value ?: return
        if (!current.playing) {
            if (!fade) cancelPendingTail()
            return
        }
        val point = pausePoint(current) ?: return stop()
        halt(fade)
        resumeAt = point
        _playback.value =
            current.copy(stepIndex = point.step, iteration = point.iteration, playing = false)
    }

    /**
     * Resumes after [pause], replaying the paused Iteration from its Key Chord.
     *
     * @return whether the Programme is playing.
     */
    fun resume(): Boolean {
        val current = _playback.value ?: return false
        if (current.playing) return true
        val point = resumeAt ?: ResumePoint(step = current.stepIndex, frame = 0L)
        return startAt(base = current, point = point)
    }

    /** Plays the next Step that fits from its Announcement, or stops after the last Step. */
    fun next() {
        val current = _playback.value ?: return
        val target = current.programme.nextStep(stepNow(current), current.range) ?: return stop()
        startAt(base = current, point = ResumePoint(step = target, frame = 0L))
    }

    /** Plays the previous Step that fits from its Announcement; on the first, restarts it. */
    fun previous() {
        val current = _playback.value ?: return
        val target = current.programme.previousStep(stepNow(current), current.range)
        startAt(base = current, point = ResumePoint(step = target, frame = 0L))
    }

    /**
     * Stops at once. A playing Programme silences and releases the device output; a paused
     * one leaves it alone, since another tool may be sounding through it now.
     */
    fun stop() {
        preparing?.cancel()
        val current = _playback.value ?: return
        if (current.playing) {
            halt(fade = false)
        } else {
            cancelPendingTail()
            cancelLoop()
        }
        resumeAt = null
        _playback.value = null
    }

    private fun startAt(base: Playback, point: ResumePoint): Boolean {
        cancelLoop()
        output.fadeOut()
        val target = base.copy(stepIndex = point.step, iteration = point.iteration, playing = true)
        if (!output.start()) {
            resumeAt = point
            _playback.value = target.copy(playing = false)
            return false
        }
        resumeAt = null
        startedAt = point
        _playback.value = target
        loop = scope.launch { run(base = target, point = point) }
        return true
    }

    private fun halt(fade: Boolean) {
        cancelLoop()
        if (fade) {
            output.fadeOut()
            tail = scope.launch {
                delay(PAUSE_TAIL_MS)
                output.stop()
                tail = null
            }
        } else {
            output.silence()
            output.stop()
        }
    }

    /**
     * When [pause] with `fade = false` finds the Programme already paused, a pending
     * [halt]'s [tail] would otherwise stop the output later, possibly after another tool has
     * started it. Cancels that tail and, if one was pending, silences and stops at once.
     */
    private fun cancelPendingTail() {
        val pending = tail?.takeIf { it.isActive } ?: return
        pending.cancel()
        tail = null
        output.silence()
        output.stop()
    }

    private fun cancelLoop() {
        loop?.cancel()
        tail?.cancel()
        tail = null
        segments.clear()
    }

    private suspend fun run(base: Playback, point: ResumePoint) {
        piano.load()
        val first = prepareStep(base = base, index = point.step) ?: return stop()
        val origin = output.framePosition() + msToFrames(START_MARGIN_MS) - point.frame
        segments.addLast(
            Segment(step = point.step, prepared = first, origin = origin, from = point.frame),
        )
        while (tick(base)) delay(TICK_MS)
    }

    /** Schedules up to the lookahead and publishes where playback is; false when done. */
    private suspend fun tick(base: Playback): Boolean {
        if (output.hasFailed()) {
            pause(fade = false)
            return false
        }
        val now = output.framePosition()
        val horizon = now + msToFrames(LOOKAHEAD_MS)
        extendTo(base = base, horizon = horizon)
        segments.forEach { scheduleUntil(segment = it, horizon = horizon) }
        val current = currentSegment(now)
        if (current.isFinal && now >= current.endFrame + msToFrames(STEP_GAP_MS)) {
            stop()
            return false
        }
        _playback.value = base.copy(stepIndex = current.step, iteration = current.iterationAt(now))
        return true
    }

    /** Appends the next Step that fits once its origin comes inside the window. */
    private suspend fun extendTo(base: Playback, horizon: Long) {
        val last = segments.last()
        val nextOrigin = last.endFrame + msToFrames(STEP_GAP_MS)
        if (last.isFinal || nextOrigin >= horizon) return
        val step = base.programme.nextStep(last.step, base.range)
        val prepared = step?.let { prepareStep(base = base, index = it) }
        if (step == null || prepared == null) {
            last.isFinal = true
            return
        }
        val origin = maxOf(nextOrigin, output.framePosition() + msToFrames(START_MARGIN_MS))
        segments.addLast(Segment(step = step, prepared = prepared, origin = origin, from = 0L))
    }

    private fun scheduleUntil(segment: Segment, horizon: Long) {
        val until = horizon - segment.origin
        if (until <= segment.scheduledUntil) return
        segment.prepared.timeline.eventsBetween(from = segment.scheduledUntil, until = until)
            .forEach { schedule(event = it, segment = segment) }
        segment.scheduledUntil = until
    }

    private fun schedule(event: TimelineEvent, segment: Segment) {
        val frame = segment.origin + event.startFrame
        when (event) {
            is AnnouncementEvent -> segment.prepared.clip?.let {
                output.schedule(id = it.id, frame = frame, gain = ANNOUNCEMENT_GAIN)
            }

            is PianoNoteEvent -> {
                val key = piano.keyFor(event.pitch)
                output.schedule(
                    id = key.id,
                    frame = frame,
                    gain = gainOf(event.part),
                    rate = key.rate,
                    lengthFrames = event.lengthFrames,
                )
            }
        }
    }

    /** The latest segment whose origin has passed; drops the ones before it. */
    private fun currentSegment(now: Long): Segment {
        while (segments.size > 1 && segments[1].origin <= now) segments.removeFirst()
        return segments.first()
    }

    /**
     * Where a pause now resumes: the sounding Iteration's first frame, the Step's start, or
     * the next Step when paused in the gap; null when paused after the last Step ended.
     * Never earlier than the frame the segment resumed from, so a pause inside the start
     * margin doesn't slip back an Iteration. Before the first tick schedules anything (the
     * piano or an Announcement still loading), the point playback started from.
     */
    private fun pausePoint(current: Playback): ResumePoint? {
        if (segments.isEmpty()) {
            return startedAt ?: ResumePoint(step = current.stepIndex, frame = 0L)
        }
        val now = output.framePosition()
        val segment = currentSegment(now)
        val timeline = segment.prepared.timeline
        val frame = timeline.resumeFrame(maxOf(now - segment.origin, segment.from))
        if (frame < timeline.lengthFrames) {
            val iteration = timeline.iterationAt(frame)?.index
            return ResumePoint(step = segment.step, frame = frame, iteration = iteration)
        }
        val next = current.programme.nextStep(segment.step, current.range) ?: return null
        return ResumePoint(step = next, frame = 0L)
    }

    private fun stepNow(current: Playback): Int =
        if (current.playing && segments.isNotEmpty()) {
            currentSegment(output.framePosition()).step
        } else {
            current.stepIndex
        }

    private suspend fun prepareStep(base: Playback, index: Int): PreparedStep? {
        val step = base.programme.steps[index]
        val clip = announcements.prepare(step.soundId)
        val timeline = buildStepTimeline(
            step = step,
            range = base.range,
            announcementFrames = clip?.lengthFrames ?: 0L,
        ) ?: return null
        return PreparedStep(timeline = timeline, clip = clip)
    }

    private suspend fun prepareAnnouncements(programme: Programme, range: Range) {
        programme.steps
            .filter { it.roundTrip(range) is RoundTrip.Fits }
            .map { it.soundId }
            .distinct()
            .forEach { announcements.prepare(it) }
    }

    private fun gainOf(part: PianoPart): Float = when (part) {
        PianoPart.KEY_CHORD -> CHORD_GAIN
        PianoPart.DEMO, PianoPart.GUIDE_MELODY -> MELODY_GAIN
    }

    /** Timing and loudness. */
    companion object {
        /** Gap between a press and the first sound. */
        const val START_MARGIN_MS: Long = 100L

        /** How far ahead sounds are handed to the engine. */
        const val LOOKAHEAD_MS: Long = 1_000L

        /** How often the schedule is topped up. */
        const val TICK_MS: Long = 100L

        /** Silence between the end of one Step and the next Step's Announcement. */
        const val STEP_GAP_MS: Long = 1_000L

        /** How long after a fading pause the device output is released. */
        const val PAUSE_TAIL_MS: Long = 150L

        /** Loudness of an Announcement, already levelled to a 0.7 peak. */
        const val ANNOUNCEMENT_GAIN: Float = 1f

        /** Loudness of a Demo or Guide Melody note. */
        const val MELODY_GAIN: Float = 1.5f

        /** Loudness of each note of a Key Chord. */
        const val CHORD_GAIN: Float = 0.9f
    }
}

private data class ResumePoint(val step: Int, val frame: Long, val iteration: Int? = null)

private class PreparedStep(val timeline: StepTimeline, val clip: Clip?)

private class Segment(
    val step: Int,
    val prepared: PreparedStep,
    val origin: Long,
    val from: Long,
) {
    var scheduledUntil: Long = from
    var isFinal: Boolean = false

    val endFrame: Long
        get() = origin + prepared.timeline.lengthFrames

    fun iterationAt(now: Long): Int? =
        prepared.timeline.iterationAt(maxOf(now - origin, from))?.index
}
