package dev.ddgrcf.sidekick.core.ansi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnsiParserTest {
    @Test
    fun parsesSgrColorsAndStylesAndStripsUnknownEscapes() {
        val sample = "\u001B]0;title\u0007\u001B[1;32mcodex\u001B[0m \u001B[38;5;196merr\u001B[39m \u001B[38;2;10;20;30;4mrgb\u001B[24;49m\u001B[2K\nnext"
        val lines = AnsiParser.parse(sample)
        assertEquals(2, lines.size)

        val line0 = lines[0].spans
        assertEquals("codex", line0[0].text)
        assertTrue(line0[0].style.bold)
        assertEquals(0xFFA6E3A1, line0[0].style.fgArgb)

        assertEquals(" ", line0[1].text)
        assertFalse(line0[1].style.bold)
        assertNull(line0[1].style.fgArgb)

        assertEquals("err", line0[2].text)
        assertEquals(0xFFFF0000L, line0[2].style.fgArgb)

        assertEquals("rgb", line0[4].text)
        assertTrue(line0[4].style.underline)
        assertEquals(0xFF0A141EL, line0[4].style.fgArgb)

        assertEquals("next", lines[1].spans[0].text)
    }
}
