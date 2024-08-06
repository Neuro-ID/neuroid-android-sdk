package com.neuroid.tracker.utils

import com.neuroid.tracker.NeuroID
import io.mockk.mockk
import org.junit.Test

class NIDComposeTextWatcherUtilsTest {
    @Test
    fun textChangeTest() {
        val neuroID = mockk<NeuroID>()
        val textWatcher = NIDComposeTextWatcherUtils(neuroID)

        val textChange1 = textWatcher.getTextChange("a", "ab")
        assert(textChange1.changedText == "b")
        assert(textChange1.count == 1)
        assert(textChange1.start == 1)

        val textChange2 = textWatcher.getTextChange("aa", "aaabc")
        assert(textChange2.changedText == "abc")
        assert(textChange2.count == 3)
        assert(textChange2.start == 2)

        val textChange3 = textWatcher.getTextChange("", "aaabc")
        assert(textChange3.changedText == "aaabc")
        assert(textChange3.count == 5)
        assert(textChange3.start == 0)
    }

    @Test
    fun testIsPaste() {
        val neuroID = mockk<NeuroID>()
        val textWatcher = NIDComposeTextWatcherUtils(neuroID)
        val isPaste =
            textWatcher.isPaste(
                textWatcher.getTextChange("gasdgasdgasd", "gasdgasdgasdzzzz"),
                "zzzz",
                0,
            )
        assert(isPaste)

        val isPaste2 =
            textWatcher.isPaste(
                textWatcher.getTextChange("gasdgasdgasd", "gasdgasdgasdzzzzzzzz"),
                "zzzz",
                "zzzz".hashCode(),
            )
        assert(!isPaste2)

        val isPaste3 =
            textWatcher.isPaste(
                textWatcher.getTextChange("", "zzzz"),
                "zzzz",
                0)
        assert(isPaste3)

        val isPaste4 =
            textWatcher.isPaste(
                textWatcher.getTextChange("", ""),
                "",
                0)
        assert(!isPaste4)

        val isPaste5 =
            textWatcher.isPaste(
                textWatcher.getTextChange("a", "ab"),
                "",
                0)
        assert(!isPaste5)
    }
}