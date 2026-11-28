package dev.ddgrcf.sidekick

import dev.ddgrcf.sidekick.core.store.MessageEntity
import dev.ddgrcf.sidekick.data.UiPart
import dev.ddgrcf.sidekick.ui.ChatTurn
import dev.ddgrcf.sidekick.ui.ToolActivityKind
import dev.ddgrcf.sidekick.ui.TurnWorkStep
import dev.ddgrcf.sidekick.ui.cleanUserPromptText
import dev.ddgrcf.sidekick.ui.groupMessagesIntoChatTurns
import dev.ddgrcf.sidekick.ui.sanitizeStreamingDraftText
import dev.ddgrcf.sidekick.ui.summarizeTurnWorkLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTurnTranslatorTest {
    @Test
    fun stripsSystemAndHarnessEnvelopesAndMergesMultiStepAssistantTurn() {
        val partsByKey = mapOf(
            "sys" to listOf(UiPart.Text("You are Codex...")),
            "env" to listOf(UiPart.Text("<environment_context><cwd>/home/r</cwd></environment_context>")),
            "u1" to listOf(UiPart.Text("<system-reminder>skip</system-reminder>\n修复扫码逻辑")),
            "a1" to listOf(
                UiPart.Thinking("Inspect PairScreen"),
                UiPart.ToolCall("c1", "read_file", """{"Path":"/repo/PairScreen.kt","StartLine":1,"EndLine":80}""", "TOOL_CALL_STATUS_COMPLETED"),
            ),
            "t1" to listOf(UiPart.ToolResult("c1", "package dev.ddgrcf...", isError = false, truncated = false)),
            "a2" to listOf(
                UiPart.ToolCall("c2", "edit_file", """{"Path":"/repo/PairScreen.kt","Target":"old","Replacement":"new1\nnew2"}""", "TOOL_CALL_STATUS_COMPLETED"),
            ),
            "t2" to listOf(UiPart.ToolResult("c2", "ok", isError = false, truncated = false)),
            "a3" to listOf(UiPart.Text("已经升级到高清扫码管线。")),
        )

        val raw = listOf(
            MessageEntity("b1", "conv1", 1, "ROLE_SYSTEM", "sys", 1L, null),
            MessageEntity("b1", "conv1", 2, "ROLE_USER", "env", 2L, null),
            MessageEntity("b1", "conv1", 3, "ROLE_USER", "u1", 3L, null),
            MessageEntity("b1", "conv1", 4, "ROLE_ASSISTANT", "a1", 4L, "gpt-5.1-codex"),
            MessageEntity("b1", "conv1", 5, "ROLE_TOOL", "t1", 5L, null),
            MessageEntity("b1", "conv1", 6, "ROLE_ASSISTANT", "a2", 6L, "gpt-5.1-codex"),
            MessageEntity("b1", "conv1", 7, "ROLE_TOOL", "t2", 7L, null),
            MessageEntity("b1", "conv1", 8, "ROLE_ASSISTANT", "a3", 8L, "gpt-5.1-codex"),
        )

        val turns = groupMessagesIntoChatTurns(raw) { key -> partsByKey[key].orEmpty() }
        assertEquals(2, turns.size)

        val user = turns[0] as ChatTurn.User
        assertEquals("修复扫码逻辑", user.textParts.single().text)

        val asst = turns[1] as ChatTurn.Assistant
        assertEquals("gpt-5.1-codex", asst.model)
        assertEquals(1, asst.thinkingBlocks.size)
        assertEquals(2, asst.activities.size)
        assertEquals(ToolActivityKind.READ, asst.activities[0].kind)
        assertEquals("PairScreen.kt · L1-80", asst.activities[0].target)
        assertEquals(ToolActivityKind.EDIT, asst.activities[1].kind)
        assertEquals(2, asst.activities[1].addedLines)
        assertEquals(1, asst.activities[1].removedLines)
        assertEquals("已经升级到高清扫码管线。", asst.textBlocks.single())
        assertTrue(summarizeTurnWorkLog(asst.thinkingBlocks.size, asst.activities).contains("已执行 2 步"))
    }

    @Test
    fun cleanUserPromptExtractsUserRequestAndStripsMeta() {
        val raw = "<USER_REQUEST>\n实现流式输出\n</USER_REQUEST>\n<ADDITIONAL_METADATA>time=now</ADDITIONAL_METADATA>"
        assertEquals("实现流式输出", cleanUserPromptText(raw))
    }

    @Test
    fun extractsAskUserQuestionIntoInteractivePromptAndSanitizesWorkingBorders() {
        val qJson = """
            {"questions":[{"header":"意图","question":"看到「女孩」，你想让我做什么？","options":[{"label":"生成一张女孩的图片","description":"用图像模型画图"},{"label":"写一段关于女孩的文字","description":"写小说片段"}]}]}
        """.trimIndent()
        val partsByKey = mapOf(
            "a1" to listOf(
                UiPart.Text("「女孩」这个词太短，我不确定你想让我做什么。"),
                UiPart.ToolCall("q1", "ask_user_question", qJson, "TOOL_CALL_STATUS_COMPLETED"),
            ),
        )
        val turns = groupMessagesIntoChatTurns(
            listOf(MessageEntity("b1", "c1", 1, "ROLE_ASSISTANT", "a1", 1L, "deepseek/deepseek-v4.1-flash")),
        ) { partsByKey[it].orEmpty() }

        val asst = turns.single() as ChatTurn.Assistant
        assertTrue(asst.activities.isEmpty())
        assertEquals(1, asst.questions.size)
        assertEquals("意图", asst.questions[0].header)
        assertEquals(2, asst.questions[0].options.size)
        assertFalse(asst.questions[0].answered)

        assertEquals("", sanitizeStreamingDraftText("----working-----\n── ⠋ Working... ────────"))
        assertEquals("你好，这是正文。", sanitizeStreamingDraftText("----working-----\n你好，这是正文。\n── ⠙ Working... ──"))
    }

    @Test
    fun translatesAllFiveAgentToolStylesIntoSemanticTurns() {
        val cases = listOf(
            Triple("pi", UiPart.ToolCall("p1", "bash", """{"command":"go test ./..."}""", "TOOL_CALL_STATUS_COMPLETED"), ToolActivityKind.COMMAND),
            Triple("codex", UiPart.ToolCall("c1", "exec_command", """{"cmd":"git status"}""", "TOOL_CALL_STATUS_COMPLETED"), ToolActivityKind.COMMAND),
            Triple("claude", UiPart.ToolCall("cl1", "Read", """{"file_path":"/repo/App.kt"}""", "TOOL_CALL_STATUS_COMPLETED"), ToolActivityKind.READ),
            Triple("antigravity", UiPart.ToolCall("a1", "edit_file", """{"Path":"/repo/Main.go","Target":"a","Replacement":"b"}""", "TOOL_CALL_STATUS_COMPLETED"), ToolActivityKind.EDIT),
            Triple("devin", UiPart.ToolCall("d1", "grep", """{"pattern":"StreamingDraft"}""", "TOOL_CALL_STATUS_COMPLETED"), ToolActivityKind.SEARCH),
        )
        for ((agent, toolCall, expectedKind) in cases) {
            val parts = mapOf(
                "u" to listOf(UiPart.Text("task for $agent")),
                "a" to listOf(UiPart.Thinking("plan"), toolCall, UiPart.Text("done for $agent")),
            )
            val turns = groupMessagesIntoChatTurns(
                listOf(
                    MessageEntity("b1", "conv-$agent", 1, "ROLE_USER", "u", 1L, null),
                    MessageEntity("b1", "conv-$agent", 2, "ROLE_ASSISTANT", "a", 2L, agent),
                ),
            ) { parts[it].orEmpty() }
            assertEquals("agent=$agent turn count", 2, turns.size)
            val asst = turns[1] as ChatTurn.Assistant
            assertEquals("agent=$agent kind", expectedKind, asst.activities.single().kind)
            assertEquals("done for $agent", asst.textBlocks.single())
        }
    }

    @Test
    fun unifiesContinuousMultiStepExecutionWithChronologicalTimelineAndClaudeToolResult() {
        val partsByKey = mapOf(
            "u1" to listOf(UiPart.Text("帮我排查并修复连续对话分裂问题")),
            "a1" to listOf(
                UiPart.Thinking("先读取 ChatTurnTranslator.kt"),
                UiPart.Text("我先查看现有消息分组逻辑。"),
                UiPart.ToolCall("c1", "Read", """{"file_path":"/repo/ChatTurnTranslator.kt"}""", "TOOL_CALL_STATUS_COMPLETED"),
            ),
            // Legacy Claude JSONL format where tool_result arrived under ROLE_USER:
            "t1" to listOf(UiPart.ToolResult("c1", "fun groupMessagesIntoChatTurns...", isError = false, truncated = false)),
            "a2" to listOf(
                UiPart.Thinking("再修改 SessionScreen.kt 合并实时草稿"),
                UiPart.ToolCall("c2", "edit_file", """{"Path":"/repo/SessionScreen.kt","Target":"a","Replacement":"b"}""", "TOOL_CALL_STATUS_COMPLETED"),
            ),
            "t2" to listOf(UiPart.ToolResult("c2", "ok", isError = false, truncated = false)),
            "a3" to listOf(UiPart.Text("已合并连续多步执行为单一回复卡片。")),
        )
        val raw = listOf(
            MessageEntity("b1", "conv1", 1, "ROLE_USER", "u1", 1L, null),
            MessageEntity("b1", "conv1", 2, "ROLE_ASSISTANT", "a1", 2L, "claude-sonnet-4"),
            MessageEntity("b1", "conv1", 3, "ROLE_USER", "t1", 3L, null),
            MessageEntity("b1", "conv1", 4, "ROLE_ASSISTANT", "a2", 4L, "claude-sonnet-4"),
            MessageEntity("b1", "conv1", 5, "ROLE_TOOL", "t2", 5L, null),
            MessageEntity("b1", "conv1", 6, "ROLE_ASSISTANT", "a3", 6L, "claude-sonnet-4"),
        )

        val turns = groupMessagesIntoChatTurns(raw) { partsByKey[it].orEmpty() }
        assertEquals(2, turns.size)
        val asst = turns[1] as ChatTurn.Assistant
        assertEquals(listOf("已合并连续多步执行为单一回复卡片。"), asst.textBlocks)
        assertEquals(5, asst.timeline.size)
        assertTrue(asst.timeline[0] is TurnWorkStep.Thinking)
        assertEquals("先读取 ChatTurnTranslator.kt", (asst.timeline[0] as TurnWorkStep.Thinking).text)
        assertTrue(asst.timeline[1] is TurnWorkStep.Note)
        assertEquals("我先查看现有消息分组逻辑。", (asst.timeline[1] as TurnWorkStep.Note).text)
        assertTrue(asst.timeline[2] is TurnWorkStep.Activity)
        assertEquals(ToolActivityKind.READ, (asst.timeline[2] as TurnWorkStep.Activity).activity.kind)
        assertFalse((asst.timeline[2] as TurnWorkStep.Activity).activity.isRunning)
        assertTrue(asst.timeline[3] is TurnWorkStep.Thinking)
        assertTrue(asst.timeline[4] is TurnWorkStep.Activity)
        assertEquals(ToolActivityKind.EDIT, (asst.timeline[4] as TurnWorkStep.Activity).activity.kind)
    }

    @Test
    fun keepsRunningStateAlongsideStreamingPartialToolOutput() {
        val partsByKey = mapOf(
            "u1" to listOf(UiPart.Text("运行测试")),
            "a1" to listOf(UiPart.ToolCall("c1", "bash", """{"command":"go test ./..."}""", "TOOL_CALL_STATUS_RUNNING")),
            "t1" to listOf(UiPart.ToolResult("c1", "=== RUN TestStreaming\n", isError = true, truncated = false)),
        )
        val raw = listOf(
            MessageEntity("b1", "conv-run", 1, "ROLE_USER", "u1", 1L, null),
            MessageEntity("b1", "conv-run", 2, "ROLE_ASSISTANT", "a1", 2L, "codex"),
            MessageEntity("b1", "conv-run", 3, "ROLE_TOOL", "t1", 3L, null),
        )
        val turns = groupMessagesIntoChatTurns(raw) { partsByKey[it].orEmpty() }
        val asst = turns[1] as ChatTurn.Assistant
        val act = asst.activities.single()
        assertTrue(act.isRunning)
        assertFalse(act.isError)
        assertEquals("执行中", act.statusLabel)
        assertEquals("=== RUN TestStreaming", act.detailOutput)
    }
}
