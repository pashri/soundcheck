package org.pashri.soundcheck.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorStateTest {
    @Test
    fun `something to show is ready`() {
        assertEquals(EditorState.Ready("x"), readyOrGone("x"))
    }

    @Test
    fun `nothing to show means the subject is gone`() {
        assertEquals(EditorState.Gone, readyOrGone<String>(null))
    }
}
