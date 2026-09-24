package org.pashri.soundcheck.audio

/**
 * A [FocusGate] the test controls.
 *
 * @property grant whether [acquire] succeeds.
 */
class FakeFocusGate(var grant: Boolean = true) : FocusGate {
    private var onLost: (() -> Unit)? = null

    /** Whether focus is currently held. */
    var held: Boolean = false
        private set

    override fun acquire(onLost: () -> Unit): Boolean {
        if (!grant) return false
        this.onLost = onLost
        held = true
        return true
    }

    override fun release() {
        onLost = null
        held = false
    }

    /** Simulates another app, such as a phone call, taking focus. */
    fun loseFocus() {
        onLost?.invoke()
    }
}
