package org.pashri.soundcheck.ui.programme

/**
 * Where a dragged row lands: it passes a neighbour once it has moved more than half of that
 * neighbour's height.
 *
 * @param from the row's position before the drag.
 * @param dragPx how far it was dragged, downwards positive.
 * @param heights every row's height in pixels, in order.
 * @return the row's new position, within the list.
 */
fun dropIndex(from: Int, dragPx: Float, heights: List<Int>): Int {
    var index = from
    var remaining = dragPx
    while (index < heights.lastIndex && remaining > heights[index + 1] / 2f) {
        remaining -= heights[index + 1]
        index++
    }
    while (index > 0 && -remaining > heights[index - 1] / 2f) {
        remaining += heights[index - 1]
        index--
    }
    return index
}
