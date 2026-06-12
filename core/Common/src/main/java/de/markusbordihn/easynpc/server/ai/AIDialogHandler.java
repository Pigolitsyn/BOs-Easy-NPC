/*
 * Copyright 2023 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.easynpc.server.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.network.NetworkHandlerManager;
import de.markusbordihn.easynpc.network.message.client.NPCSpeakMessage;
import de.markusbordihn.easynpc.network.message.client.ReceiveAIMessageMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AIDialogHandler {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static final int MAX_HISTORY_SIZE = 20;
  private static final HttpClient HTTP_CLIENT =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private static final Map<String, List<ChatMessage>> conversationHistory =
      new ConcurrentHashMap<>();
  private static final Map<String, QuestRail> questRails = new ConcurrentHashMap<>();

  private static final String PROTOCOL_REMINDER =
      "system-reminder: Reply with valid JSON following the dialog protocol schema: "
          + "{\"say\": string, \"options\": [{\"id\": string, \"label\": string}], "
          + "\"question_asked\": boolean, \"answer_verdict\": \"correct\"|\"wrong\"|null, "
          + "\"stage_complete_claim\": boolean}. No other text.";

  private AIDialogHandler() {}

  public static void clearHistory(UUID npcId, UUID playerId) {
    String key = historyKey(npcId, playerId);
    log.info("[AI] Clearing conversation history for key={}", key);
    conversationHistory.remove(key);
    questRails.remove(key);
  }

  public static List<ChatMessage> getHistory(UUID npcId, UUID playerId) {
    List<ChatMessage> h = conversationHistory.get(historyKey(npcId, playerId));
    return h == null ? List.of() : List.copyOf(h);
  }

  public static void processMessage(
      UUID npcId,
      ServerPlayer serverPlayer,
      String userMessage,
      String serverUrl,
      String modelName,
      String systemPrompt,
      String apiKey,
      String questObjective,
      int requiredCorrect) {

    UUID playerId = serverPlayer.getUUID();
    log.info(
        "[AI] processMessage: npcId={} playerId={} model='{}' hasApiKey={} messageLength={}",
        npcId,
        playerId,
        modelName,
        (apiKey != null && !apiKey.isBlank()),
        userMessage != null ? userMessage.length() : 0);

    // Validate URL
    if (serverUrl == null || serverUrl.isBlank()) {
      log.error("[AI] Server URL not configured for NPC {}", npcId);
      NetworkHandlerManager.sendMessageToPlayer(
          new ReceiveAIMessageMessage(npcId, "assistant", "AI server URL is not configured."),
          serverPlayer);
      return;
    }
    if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
      log.error("[AI] Invalid server URL '{}' for NPC {} (must start with http:// or https://)", serverUrl, npcId);
      NetworkHandlerManager.sendMessageToPlayer(
          new ReceiveAIMessageMessage(
              npcId, "assistant", "Invalid AI server URL (must start with http:// or https://)."),
          serverPlayer);
      return;
    }

    log.debug("[AI] Server URL validated: {}", serverUrl);

    String key = historyKey(npcId, playerId);
    List<ChatMessage> history =
        conversationHistory.computeIfAbsent(key, k -> new ArrayList<>());
    history.add(new ChatMessage("user", userMessage));
    log.debug("[AI] History size after adding user message: {}", history.size());

    int relation = AIRelationManager.get(npcId, playerId);
    String augmentedPrompt = augmentSystemPrompt(systemPrompt, relation, serverPlayer.getName().getString());
    String requestJson = buildRequestJson(modelName, augmentedPrompt, history);
    log.debug("[AI] Request body: {}", requestJson);

    String normalizedUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    String endpoint = normalizedUrl + "v1/chat/completions";
    log.info("[AI] Sending request to endpoint: {}", endpoint);

    boolean hasApiKey = apiKey != null && !apiKey.isBlank();
    if (hasApiKey) {
      log.debug("[AI] Authorization header will be sent (Bearer token)");
    }

    String effectiveApiKey = hasApiKey ? apiKey : null;
    dispatchChat(endpoint, requestJson, effectiveApiKey)
        .thenAccept(
            responseBody ->
                handleChatResponse(
                    npcId,
                    serverPlayer,
                    key,
                    history,
                    endpoint,
                    modelName,
                    augmentedPrompt,
                    effectiveApiKey,
                    questObjective,
                    requiredCorrect,
                    responseBody,
                    false));
  }

  private static CompletableFuture<String> dispatchChat(
      String endpoint, String requestJson, String apiKey) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            HttpRequest request = buildHttpRequest(endpoint, requestJson, apiKey);
            log.debug("[AI] HTTP request built, sending...");

            HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String body = response.body();
            log.info(
                "[AI] HTTP response status={} bodyLength={}",
                statusCode,
                body != null ? body.length() : 0);
            log.debug("[AI] HTTP response body: {}", body);

            if (statusCode < 200 || statusCode >= 300) {
              log.error(
                  "[AI] Non-2xx response from AI service: status={} body={}", statusCode, body);
            }

            return body;
          } catch (Exception e) {
            log.error(
                "[AI] Error calling AI API at {}: {} ({})",
                endpoint,
                e.getMessage(),
                e.getClass().getSimpleName());
            return null;
          }
        });
  }

  private static void handleChatResponse(
      UUID npcId,
      ServerPlayer serverPlayer,
      String key,
      List<ChatMessage> history,
      String endpoint,
      String modelName,
      String augmentedPrompt,
      String apiKey,
      String questObjective,
      int requiredCorrect,
      String responseBody,
      boolean isRetry) {

    UUID playerId = serverPlayer.getUUID();

    if (responseBody == null) {
      log.error("[AI] Response body is null — connection or timeout failure");
      NetworkHandlerManager.sendMessageToPlayer(
          new ReceiveAIMessageMessage(
              npcId, "assistant", "Sorry, I could not connect to the AI service."),
          serverPlayer);
      return;
    }

    try {
      log.debug("[AI] Parsing response JSON...");
      JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

      if (json.has("error")) {
        String errorMsg = json.getAsJsonObject("error").get("message").getAsString();
        log.error("[AI] API returned error: {}", errorMsg);
        NetworkHandlerManager.sendMessageToPlayer(
            new ReceiveAIMessageMessage(npcId, "assistant", "AI error: " + errorMsg),
            serverPlayer);
        history.remove(history.size() - 1);
        return;
      }

      String rawContent =
          json.getAsJsonArray("choices")
              .get(0)
              .getAsJsonObject()
              .getAsJsonObject("message")
              .get("content")
              .getAsString();

      AIToolCallParser.Result parsed = AIToolCallParser.parse(rawContent);
      String content = parsed.cleanedText();
      if (parsed.relationDelta() != 0) {
        int updated = AIRelationManager.adjust(npcId, playerId, parsed.relationDelta());
        log.info(
            "[AI] relation adjust npc={} player={} delta={} new={}",
            npcId,
            playerId,
            parsed.relationDelta(),
            updated);
      }

      log.info("[AI] Received assistant reply (length={} retry={})", content.length(), isRetry);
      log.debug("[AI] Assistant reply: {}", content);

      AIProtocol.Reply reply = AIProtocol.parse(content);

      if (!reply.parsed() && !isRetry) {
        log.warn("[AI] Protocol parse failed, retrying once with system-reminder (npc={})", npcId);
        List<ChatMessage> retryHistory = new ArrayList<>(history);
        retryHistory.add(new ChatMessage("system", PROTOCOL_REMINDER));
        String retryJson = buildRequestJson(modelName, augmentedPrompt, retryHistory);
        dispatchChat(endpoint, retryJson, apiKey)
            .thenAccept(
                retryBody ->
                    handleChatResponse(
                        npcId,
                        serverPlayer,
                        key,
                        history,
                        endpoint,
                        modelName,
                        augmentedPrompt,
                        apiKey,
                        questObjective,
                        requiredCorrect,
                        retryBody,
                        true));
        return;
      }

      if (!reply.parsed()) {
        log.warn(
            "[AI] Protocol parse failed twice, degrading to raw text reply (npc={})", npcId);
        appendAssistantHistory(history, content);
        sendReply(serverPlayer, npcId, content, List.of());
        return;
      }

      // Model sees its own protocol: keep the raw JSON in the conversation history.
      appendAssistantHistory(history, content);
      applyQuestRail(key, npcId, serverPlayer, reply, questObjective, requiredCorrect);

      String say = reply.say() == null || reply.say().isBlank() ? content : reply.say();
      sendReply(serverPlayer, npcId, say, reply.options());
    } catch (Exception e) {
      log.error("[AI] Error parsing AI response: {} — raw body: {}", e.getMessage(), responseBody);
      NetworkHandlerManager.sendMessageToPlayer(
          new ReceiveAIMessageMessage(npcId, "assistant", "Error parsing AI response."),
          serverPlayer);
    }
  }

  private static void appendAssistantHistory(List<ChatMessage> history, String content) {
    history.add(new ChatMessage("assistant", content));
    while (history.size() > MAX_HISTORY_SIZE) {
      history.remove(0);
    }
    log.debug("[AI] History size after reply: {}", history.size());
  }

  private static void sendReply(
      ServerPlayer serverPlayer, UUID npcId, String say, List<AIProtocol.Option> options) {
    // TODO(next task): extend ReceiveAIMessageMessage payload to carry the dialog options.
    if (!options.isEmpty()) {
      log.debug("[AI] Reply carries {} options (payload support pending): {}", options.size(), options);
    }
    NetworkHandlerManager.sendMessageToPlayer(
        new ReceiveAIMessageMessage(npcId, "assistant", say), serverPlayer);
  }

  private static void applyQuestRail(
      String key,
      UUID npcId,
      ServerPlayer serverPlayer,
      AIProtocol.Reply reply,
      String questObjective,
      int requiredCorrect) {

    QuestRail rail = questRails.computeIfAbsent(key, k -> new QuestRail());
    synchronized (rail) {
      if ("correct".equals(reply.answerVerdict()) && rail.pendingQuestion) {
        rail.correctCount++;
        rail.pendingQuestion = false;
        log.info(
            "[AI] Quest rail: correct answer npc={} count={}/{}",
            npcId,
            rail.correctCount,
            requiredCorrect);
      } else if ("wrong".equals(reply.answerVerdict())) {
        rail.pendingQuestion = false;
        log.info("[AI] Quest rail: wrong answer npc={} count={}", npcId, rail.correctCount);
      }
      if (reply.questionAsked()) {
        rail.pendingQuestion = true;
      }

      boolean hasObjective = questObjective != null && !questObjective.isBlank();
      boolean completedNow = rail.correctCount >= requiredCorrect;

      if (reply.stageCompleteClaim() && !completedNow) {
        log.debug(
            "[AI] Ignoring stage_complete_claim: count={}/{} npc={}",
            rail.correctCount,
            requiredCorrect,
            npcId);
      }

      if (hasObjective && !rail.completed && completedNow) {
        rail.completed = true;
        var server = serverPlayer.level().getServer();
        Runnable apply = () -> setQuestScore(serverPlayer, questObjective);
        if (server != null) {
          server.execute(apply);
        } else {
          apply.run();
        }
      }
    }
  }

  private static void setQuestScore(ServerPlayer serverPlayer, String objectiveName) {
    Scoreboard scoreboard = serverPlayer.level().getScoreboard();
    Objective objective = scoreboard.getObjective(objectiveName);
    if (objective == null) {
      log.warn(
          "[AI] Quest objective '{}' not found on scoreboard for player {} — skipping completion",
          objectiveName,
          serverPlayer.getName().getString());
      return;
    }
    scoreboard.getOrCreatePlayerScore(serverPlayer, objective).set(1);
    log.info(
        "[AI] Quest complete: set scoreboard objective '{}' to 1 for player {}",
        objectiveName,
        serverPlayer.getName().getString());
  }

  static class QuestRail {
    boolean pendingQuestion;
    int correctCount;
    boolean completed;
  }

  static String buildRequestJson(String modelName, String systemPrompt, List<ChatMessage> history) {
    JsonObject requestBody = new JsonObject();
    String effectiveModel = (modelName == null || modelName.isEmpty()) ? "assistant" : modelName;
    requestBody.addProperty("model", effectiveModel);
    requestBody.addProperty("stream", false);

    JsonArray messages = new JsonArray();
    if (systemPrompt != null && !systemPrompt.isEmpty()) {
      JsonObject sysMsg = new JsonObject();
      sysMsg.addProperty("role", "system");
      sysMsg.addProperty("content", systemPrompt);
      messages.add(sysMsg);
    }
    for (ChatMessage msg : history) {
      JsonObject m = new JsonObject();
      m.addProperty("role", msg.role());
      m.addProperty("content", msg.content());
      messages.add(m);
    }
    requestBody.add("messages", messages);
    return requestBody.toString();
  }

  static HttpRequest buildHttpRequest(String endpoint, String requestJson, String apiKey) {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .timeout(Duration.ofSeconds(30));
    if (apiKey != null && !apiKey.isBlank()) {
      builder = builder.header("Authorization", "Bearer " + apiKey);
    }
    return builder.build();
  }

  private static String historyKey(UUID npcId, UUID playerId) {
    return npcId + ":" + playerId;
  }

  /**
   * Stateless event-triggered AI call. Bypasses conversation history (no follow-up expected).
   * Result fed into the supplied callback on the server thread of the npc's level when present;
   * callers may also broadcast directly. {@code radius} controls how far the resulting speech
   * bubble is broadcast on the client.
   */
  public static void processEventMessage(
      UUID npcId,
      ServerLevel level,
      String eventPrompt,
      String serverUrl,
      String modelName,
      String systemPrompt,
      String apiKey,
      int speechBubbleRadius,
      Consumer<String> onReply) {

    if (serverUrl == null || serverUrl.isBlank()
        || (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://"))) {
      log.warn("[AI][event] invalid server URL for npc {}", npcId);
      return;
    }

    List<ChatMessage> singleShot = new ArrayList<>();
    singleShot.add(new ChatMessage("user", eventPrompt));
    String requestJson = buildRequestJson(modelName, systemPrompt, singleShot);
    String normalizedUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
    String endpoint = normalizedUrl + "v1/chat/completions";
    boolean hasApiKey = apiKey != null && !apiKey.isBlank();
    log.info(
        "[AI][event] dispatch npc={} endpoint={} model='{}' hasKey={} promptLen={}",
        npcId,
        endpoint,
        modelName,
        hasApiKey,
        eventPrompt == null ? 0 : eventPrompt.length());

    CompletableFuture.supplyAsync(
            () -> {
              try {
                HttpRequest request =
                    buildHttpRequest(endpoint, requestJson, hasApiKey ? apiKey : null);
                HttpResponse<String> response =
                    HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                  log.warn(
                      "[AI][event] non-2xx status={} body={}",
                      response.statusCode(),
                      response.body());
                  return null;
                }
                return response.body();
              } catch (Exception e) {
                log.warn("[AI][event] call failed: {}", e.getMessage());
                return null;
              }
            })
        .thenAccept(
            body -> {
              String rawContent = extractContent(body);
              if (rawContent == null || rawContent.isEmpty()) {
                log.warn("[AI][event] empty content for npc={}", npcId);
                return;
              }
              AIToolCallParser.Result parsed = AIToolCallParser.parse(rawContent);
              String content = parsed.cleanedText();
              if (content.isEmpty()) {
                return;
              }
              log.info(
                  "[AI][event] reply npc={} contentLen={} relationDelta={}",
                  npcId,
                  content.length(),
                  parsed.relationDelta());
              if (onReply != null) {
                onReply.accept(content);
              }
              if (level != null) {
                NPCSpeakMessage msg = new NPCSpeakMessage(npcId, content);
                int sent = 0;
                for (ServerPlayer p : level.players()) {
                  if (isWithinRadius(p, npcId, level, speechBubbleRadius)) {
                    NetworkHandlerManager.sendMessageToPlayer(msg, p);
                    sent++;
                  }
                }
                log.info(
                    "[AI][event] broadcast npc={} radius={} recipients={}",
                    npcId,
                    speechBubbleRadius,
                    sent);
              }
            });
  }

  static String augmentSystemPrompt(String basePrompt, int relation, String playerName) {
    StringBuilder sb = new StringBuilder();
    if (basePrompt != null && !basePrompt.isBlank()) {
      sb.append(basePrompt).append("\n\n");
    }
    sb.append("Current relation with ")
        .append(playerName == null || playerName.isBlank() ? "the player" : playerName)
        .append(": ")
        .append(relation)
        .append(" (")
        .append(AIRelationManager.describe(relation))
        .append(") on a scale of -100..+100. ")
        .append("Reflect this in your tone.\n")
        .append("Tools: include literal tag [adjust_relation:+N] or [adjust_relation:-N] ")
        .append("(N in 1..20) at the end of your reply when the player's words clearly improve ")
        .append("or worsen your view of them. Do not mention the tag itself.\n");
    return sb.toString();
  }

  static String extractContent(String responseBody) {
    if (responseBody == null) {
      return null;
    }
    try {
      JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
      if (json.has("error")) {
        return null;
      }
      return json.getAsJsonArray("choices")
          .get(0)
          .getAsJsonObject()
          .getAsJsonObject("message")
          .get("content")
          .getAsString();
    } catch (Exception e) {
      log.warn("[AI][event] parse failed: {}", e.getMessage());
      return null;
    }
  }

  private static boolean isWithinRadius(
      ServerPlayer player, UUID npcId, ServerLevel level, int radius) {
    var npc = level.getEntity(npcId);
    if (npc == null) {
      return false;
    }
    double dx = npc.getX() - player.getX();
    double dy = npc.getY() - player.getY();
    double dz = npc.getZ() - player.getZ();
    return (dx * dx + dy * dy + dz * dz) <= (double) radius * radius;
  }

  public record ChatMessage(String role, String content) {}
}
