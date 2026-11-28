package dev.ddgrcf.sidekick

import dev.ddgrcf.sidekick.ui.DiffLineKind
import dev.ddgrcf.sidekick.ui.parseSinglePatchSnippet
import dev.ddgrcf.sidekick.ui.parseUnifiedDiffByFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiffPreviewTest {
    @Test
    fun parseUnifiedDiffSplitsFilesMergesHunksAndTracksLineNumbers() {
        val raw = """
            diff --git a/lua/a.lua b/lua/a.lua
            index 111..222 100644
            --- a/lua/a.lua
            +++ b/lua/a.lua
            @@ -10,2 +10,3 @@ local M = {}
             local x = 1
            -local y = 2
            +local y = 3
            +local z = 4
            diff --git a/lua/a.lua b/lua/a.lua
            index 222..333 100644
            --- a/lua/a.lua
            +++ b/lua/a.lua
            @@ -20,1 +21,2 @@ return M
             -- tail
            +M.ok = true
            diff --git a/new_file.lua b/new_file.lua
            new file mode 100644
            --- /dev/null
            +++ b/new_file.lua
            @@ -0,0 +1,2 @@
            +local a = 1
            +return a
        """.trimIndent()

        val parsed = parseUnifiedDiffByFile(raw)
        assertEquals(setOf("lua/a.lua", "new_file.lua"), parsed.keys)

        val a = parsed.getValue("lua/a.lua")
        assertEquals(3, a.added)
        assertEquals(1, a.removed)
        val firstRemove = a.lines.first { it.kind == DiffLineKind.REMOVE }
        assertEquals(11, firstRemove.oldLine)
        assertEquals(null, firstRemove.newLine)
        val addedTail = a.lines.last { it.kind == DiffLineKind.ADD }
        assertEquals(22, addedTail.newLine)

        val untracked = parsed.getValue("new_file.lua")
        assertEquals(2, untracked.added)
        assertEquals(0, untracked.removed)
        assertEquals(1, untracked.lines.first { it.kind == DiffLineKind.ADD }.newLine)
    }

    @Test
    fun parseSinglePatchSnippetHandlesSyntheticAndUnifiedDiffs() {
        val synthetic = parseSinglePatchSnippet("- old line\n+ new line\n+ extra")
        assertEquals(3, synthetic.size)
        assertEquals(DiffLineKind.REMOVE, synthetic[0].kind)
        assertEquals(DiffLineKind.ADD, synthetic[1].kind)
        assertTrue(synthetic[0].oldLine == null && synthetic[1].newLine == null)
    }
}
