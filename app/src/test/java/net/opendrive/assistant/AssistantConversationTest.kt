package net.opendrive.assistant

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AssistantConversationTest {
    private val tools get() = JSONArray().put(JSONObject().put("name", "car_media"))
    private fun response(vararg items: JSONObject) = JSONObject().put("status", "completed").put("output", JSONArray(items.toList()))
    private fun message(text: String) = JSONObject().put("type", "message").put("role", "assistant")
        .put("content", JSONArray().put(JSONObject().put("type", "output_text").put("text", text)))
    private fun call(name: String = "car_media") = JSONObject().put("type", "function_call").put("name", name)
        .put("call_id", "call_1").put("arguments", "{\"action\":\"pause\"}")

    @Test fun sendsToolResultsBackAndRetainsCompleteFollowUpContextWithoutServerStorage() = runBlocking<Unit> {
        val conversation = AssistantConversation()
        val requests = mutableListOf<JSONObject>()
        var executed = 0
        val request: suspend (JSONObject) -> JSONObject = { body ->
            requests += JSONObject(body.toString())
            when (requests.size) {
                1 -> response(call())
                2 -> response(message("Paused."))
                else -> response(message("It is paused."))
            }
        }
        val execute: suspend (String, JSONObject) -> JSONObject = { name, args ->
            assertEquals("car_media", name); assertEquals("pause", args.getString("action")); executed++
            JSONObject().put("ok", true)
        }
        assertEquals("Paused.", conversation.answer("Pause", "gpt-5-mini", "", "en", tools, request, execute))
        assertEquals("It is paused.", conversation.answer("And now?", "gpt-5-mini", "", "en", tools, request, execute))
        assertEquals(1, executed)
        assertFalse(requests[0].getBoolean("store"))
        assertFalse(requests[0].getBoolean("parallel_tool_calls"))
        assertEquals("function_call_output", requests[1].getJSONArray("input").getJSONObject(2).getString("type"))
        assertEquals("call_1", requests[1].getJSONArray("input").getJSONObject(2).getString("call_id"))
        assertEquals(5, requests[2].getJSONArray("input").length())
    }

    @Test fun unsupportedToolNeverRuns() = runBlocking<Unit> {
        var requested = 0
        var executed = false
        AssistantConversation().answer("Send a message", "model", "", "en", tools,
            { if (requested++ == 0) response(call("car_reply")) else response(message("Not available.")) },
            { _, _ -> executed = true; JSONObject() })
        assertFalse(executed)
    }

    @Test fun incompleteResponsesAndParallelCallsCannotExecuteActions() = runBlocking<Unit> {
        for (invalid in listOf(response(call()).put("status", "incomplete"), response(call(), call()))) {
            var executed = false
            try {
                AssistantConversation().answer("Pause", "model", "", "en", tools, { invalid },
                    { _, _ -> executed = true; JSONObject() })
                fail("Expected failure")
            } catch (_: IllegalStateException) { }
            assertFalse(executed)
        }
    }

    @Test fun failedAndCancelledTurnsAreNotReplayedAndNewConversationClearsMemory() = runBlocking<Unit> {
        val conversation = AssistantConversation()
        for (exception in listOf(IllegalStateException("offline"), CancellationException("cancelled"))) {
            try {
                conversation.answer("failed", "model", "", "en", tools, { throw exception }, { _, _ -> JSONObject() })
                fail("Expected failure")
            } catch (_: Exception) { }
        }
        val request: suspend (JSONObject) -> JSONObject = {
            assertEquals(1, it.getJSONArray("input").length()); response(message("OK"))
        }
        conversation.answer("hello", "model", "", "en", tools, request, { _, _ -> JSONObject() })
        conversation.clear()
        conversation.answer("new", "model", "", "en", tools, request, { _, _ -> JSONObject() })
    }

    @Test fun repeatedToolsAreBoundedAndLastRequestDisablesTools() = runBlocking<Unit> {
        var requests = 0
        var executed = 0
        val answer = AssistantConversation().answer("Pause", "model", "", "en", tools,
            {
                requests++
                if (requests < 5) response(call()) else {
                    assertEquals("none", it.getString("tool_choice")); response(message("Done"))
                }
            }, { _, _ -> executed++; JSONObject().put("ok", true) })
        assertEquals("Done", answer); assertEquals(5, requests); assertEquals(1, executed)
    }

    @Test fun historyEvictionRetainsOnlyWholeTurns() = runBlocking<Unit> {
        val conversation = AssistantConversation()
        repeat(10) { index ->
            conversation.answer("Question $index", "model", "", "en", tools,
                { body ->
                    val input = body.getJSONArray("input")
                    assertEquals(minOf(index, 6) * 2 + 1, input.length())
                    assertEquals("user", input.getJSONObject(0).getString("role"))
                    response(message("Answer $index"))
                }, { _, _ -> JSONObject() })
        }
    }

    @Test fun cancellationAfterAResponsePreventsItsActionFromRunning() = runBlocking<Unit> {
        var executed = false
        val task = launch {
            AssistantConversation().answer("Pause", "gpt-6.1-sol", "", "en", tools,
                { body ->
                    assertEquals("low", body.getJSONObject("reasoning").getString("effort"))
                    assertEquals("reasoning.encrypted_content", body.getJSONArray("include").getString(0))
                    currentCoroutineContext().cancel()
                    response(call())
                }, { _, _ -> executed = true; JSONObject() })
        }
        task.join()
        assertTrue(task.isCancelled)
        assertFalse(executed)
    }
}
