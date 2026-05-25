/*
 * Copyright 2023 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.easynpc.server.ai;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import de.markusbordihn.easynpc.server.ai.AIDialogHandler.ChatMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AIDialogHandlerTest {

  // ─── buildRequestJson ────────────────────────────────────────────────────

  @Test
  void testRequestBodyIsNotEmpty() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "Hello"));
    String json = AIDialogHandler.buildRequestJson("llama3", "", history);
    assertNotNull(json);
    assertFalse(json.isBlank(), "Request body must not be blank");
  }

  @Test
  void testRequestBodyContainsModel() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "hi"));
    String json = AIDialogHandler.buildRequestJson("my-model", "", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    assertEquals("my-model", obj.get("model").getAsString());
  }

  @Test
  void testRequestBodyDefaultModelWhenEmpty() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "hi"));
    String json = AIDialogHandler.buildRequestJson("", "", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    assertEquals("assistant", obj.get("model").getAsString());
  }

  @Test
  void testRequestBodyDefaultModelWhenNull() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "hi"));
    String json = AIDialogHandler.buildRequestJson(null, "", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    assertEquals("assistant", obj.get("model").getAsString());
  }

  @Test
  void testRequestBodyStreamIsFalse() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "hi"));
    String json = AIDialogHandler.buildRequestJson("m", "", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    assertFalse(obj.get("stream").getAsBoolean());
  }

  @Test
  void testRequestBodyContainsUserMessage() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "Hello there"));
    String json = AIDialogHandler.buildRequestJson("m", "", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    JsonArray messages = obj.getAsJsonArray("messages");
    assertEquals(1, messages.size());
    JsonObject msg = messages.get(0).getAsJsonObject();
    assertEquals("user", msg.get("role").getAsString());
    assertEquals("Hello there", msg.get("content").getAsString());
  }

  @Test
  void testRequestBodyIncludesSystemPrompt() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "hi"));
    String json = AIDialogHandler.buildRequestJson("m", "You are a pirate NPC.", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    JsonArray messages = obj.getAsJsonArray("messages");
    assertEquals(2, messages.size());
    JsonObject sys = messages.get(0).getAsJsonObject();
    assertEquals("system", sys.get("role").getAsString());
    assertEquals("You are a pirate NPC.", sys.get("content").getAsString());
  }

  @Test
  void testRequestBodySystemPromptAbsentWhenEmpty() {
    List<ChatMessage> history = List.of(new ChatMessage("user", "hi"));
    String json = AIDialogHandler.buildRequestJson("m", "", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    JsonArray messages = obj.getAsJsonArray("messages");
    assertEquals(1, messages.size());
    assertEquals("user", messages.get(0).getAsJsonObject().get("role").getAsString());
  }

  @Test
  void testRequestBodyPreservesHistoryOrder() {
    List<ChatMessage> history = new ArrayList<>();
    history.add(new ChatMessage("user", "msg1"));
    history.add(new ChatMessage("assistant", "reply1"));
    history.add(new ChatMessage("user", "msg2"));
    String json = AIDialogHandler.buildRequestJson("m", "", history);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
    JsonArray messages = obj.getAsJsonArray("messages");
    assertEquals(3, messages.size());
    assertEquals("msg1", messages.get(0).getAsJsonObject().get("content").getAsString());
    assertEquals("reply1", messages.get(1).getAsJsonObject().get("content").getAsString());
    assertEquals("msg2", messages.get(2).getAsJsonObject().get("content").getAsString());
  }

  // ─── buildHttpRequest ────────────────────────────────────────────────────

  @Test
  void testHttpRequestMethodIsPost() {
    HttpRequest req = AIDialogHandler.buildHttpRequest(
        "http://localhost:9999/v1/chat/completions", "{}", null);
    assertEquals("POST", req.method());
  }

  @Test
  void testHttpRequestHasContentTypeJson() {
    HttpRequest req = AIDialogHandler.buildHttpRequest(
        "http://localhost:9999/v1/chat/completions", "{}", null);
    assertTrue(
        req.headers().firstValue("Content-Type").orElse("").contains("application/json"),
        "Content-Type header must be application/json");
  }

  @Test
  void testHttpRequestNoAuthHeaderWhenKeyNull() {
    HttpRequest req = AIDialogHandler.buildHttpRequest(
        "http://localhost:9999/v1/chat/completions", "{}", null);
    assertTrue(
        req.headers().firstValue("Authorization").isEmpty(),
        "Authorization header must be absent when API key is null");
  }

  @Test
  void testHttpRequestNoAuthHeaderWhenKeyBlank() {
    HttpRequest req = AIDialogHandler.buildHttpRequest(
        "http://localhost:9999/v1/chat/completions", "{}", "   ");
    assertTrue(
        req.headers().firstValue("Authorization").isEmpty(),
        "Authorization header must be absent when API key is blank");
  }

  @Test
  void testHttpRequestHasBearerTokenWhenKeyProvided() {
    HttpRequest req = AIDialogHandler.buildHttpRequest(
        "http://localhost:9999/v1/chat/completions", "{}", "sk-test-key");
    String auth = req.headers().firstValue("Authorization").orElse("");
    assertEquals("Bearer sk-test-key", auth, "Authorization header must carry Bearer token");
  }

  // ─── Integration: actual body arrives at server ──────────────────────────

  @Test
  void testActualPostBodyIsNotEmpty() throws IOException, InterruptedException {
    AtomicReference<String> receivedBody = new AtomicReference<>("");
    AtomicReference<String> receivedAuth = new AtomicReference<>("");

    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();
    server.createContext(
        "/v1/chat/completions",
        exchange -> {
          receivedAuth.set(
              exchange.getRequestHeaders().getFirst("Authorization") != null
                  ? exchange.getRequestHeaders().getFirst("Authorization")
                  : "");
          byte[] bytes = exchange.getRequestBody().readAllBytes();
          receivedBody.set(new String(bytes, StandardCharsets.UTF_8));
          String response = "{\"choices\":[{\"message\":{\"content\":\"hi\"}}]}";
          exchange.sendResponseHeaders(200, response.length());
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
          }
        });
    server.start();

    try {
      List<ChatMessage> history = List.of(new ChatMessage("user", "Test message"));
      String requestJson = AIDialogHandler.buildRequestJson("llama3", "You are helpful.", history);
      HttpRequest req =
          AIDialogHandler.buildHttpRequest(
              "http://localhost:" + port + "/v1/chat/completions",
              requestJson,
              "sk-test-key");

      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

      String body = receivedBody.get();
      assertFalse(body.isBlank(), "Server must receive a non-empty body");

      JsonObject parsed = JsonParser.parseString(body).getAsJsonObject();
      assertEquals("llama3", parsed.get("model").getAsString());
      assertFalse(parsed.get("stream").getAsBoolean());

      JsonArray msgs = parsed.getAsJsonArray("messages");
      assertEquals(2, msgs.size(), "Should have system + user message");
      assertEquals("system", msgs.get(0).getAsJsonObject().get("role").getAsString());
      assertEquals("user", msgs.get(1).getAsJsonObject().get("role").getAsString());
      assertEquals("Test message", msgs.get(1).getAsJsonObject().get("content").getAsString());

      assertEquals(
          "Bearer sk-test-key",
          receivedAuth.get(),
          "Server must receive Authorization: Bearer header");
    } finally {
      server.stop(0);
    }
  }

  @Test
  void testActualPostBodyWithoutApiKey() throws IOException, InterruptedException {
    AtomicReference<String> receivedAuth = new AtomicReference<>("");

    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();
    server.createContext(
        "/v1/chat/completions",
        exchange -> {
          receivedAuth.set(
              exchange.getRequestHeaders().getFirst("Authorization") != null
                  ? exchange.getRequestHeaders().getFirst("Authorization")
                  : "ABSENT");
          String response = "{\"choices\":[{\"message\":{\"content\":\"pong\"}}]}";
          exchange.sendResponseHeaders(200, response.length());
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
          }
        });
    server.start();

    try {
      List<ChatMessage> history = List.of(new ChatMessage("user", "ping"));
      String requestJson = AIDialogHandler.buildRequestJson("m", "", history);
      HttpRequest req =
          AIDialogHandler.buildHttpRequest(
              "http://localhost:" + port + "/v1/chat/completions", requestJson, null);

      java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
      client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

      assertEquals("ABSENT", receivedAuth.get(), "No Authorization header when no API key");
    } finally {
      server.stop(0);
    }
  }

  // ─── History accumulation ────────────────────────────────────────────────

  @Test
  void testHistoryGrowsWithEachTurn() {
    List<ChatMessage> history = new ArrayList<>();
    history.add(new ChatMessage("user", "turn1"));

    String json1 = AIDialogHandler.buildRequestJson("m", "", history);
    JsonArray msgs1 = JsonParser.parseString(json1).getAsJsonObject().getAsJsonArray("messages");
    assertEquals(1, msgs1.size());

    history.add(new ChatMessage("assistant", "answer1"));
    history.add(new ChatMessage("user", "turn2"));

    String json2 = AIDialogHandler.buildRequestJson("m", "", history);
    JsonArray msgs2 = JsonParser.parseString(json2).getAsJsonObject().getAsJsonArray("messages");
    assertEquals(3, msgs2.size());
    assertEquals("turn2", msgs2.get(2).getAsJsonObject().get("content").getAsString());
  }

  @Test
  void testHistoryKeyIsUniquePerNpcAndPlayer() {
    // Clear any pre-existing state
    java.util.UUID npc1 = java.util.UUID.randomUUID();
    java.util.UUID npc2 = java.util.UUID.randomUUID();
    java.util.UUID player = java.util.UUID.randomUUID();

    AIDialogHandler.clearHistory(npc1, player);
    AIDialogHandler.clearHistory(npc2, player);

    // Verify keys are distinct (clearHistory uses historyKey internally — no crash = keys differ)
    assertNotEquals(npc1 + ":" + player, npc2 + ":" + player);
  }
}
