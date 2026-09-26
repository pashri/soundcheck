package org.pashri.soundcheck.audio

/**
 * A [FocusGate] the test controls.
 *
 * @property grant whether [acquire] succeeds.
 */
class FakeFocusGate(var grant: Boolean = true) : FocusGate {
    private var onLost: (() -> Unit)? = null
    private var onRegained: (() -> Unit)? = null

    /** Whether focus is currently held. */
    var held: Boolean = false
        private set

    /** How many times [acquire] was called, granted or not. */
    var acquireCount: Int = 0
        private set

    override fun acquire(onLost: () -> Unit, onRegained: () -> Unit): Boolean {
        acquireCount++
        if (!grant) return false
        this.onLost = onLost
        this.onRegained = onRegained
        held = true
        return true
    }

    override fun release() {
        onLost = null
        onRegained = null
        held = false
    }

    /** Simulates another app, such as a phone call, taking focus. */
    fun loseFocus() {
        onLost?.invoke()
    }

    /** Simulates focus coming back after a temporary loss, such as the call ending. */
    fun regainFocus() {
        onRegained?.invoke()
    }
}
