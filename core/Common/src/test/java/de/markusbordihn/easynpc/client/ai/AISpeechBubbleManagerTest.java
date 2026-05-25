/*
 * Copyright 2025 Markus Bordihn
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

package de.markusbordihn.easynpc.client.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AISpeechBubbleManagerTest {

  private final AtomicLong clock = new AtomicLong(0L);

  @BeforeEach
  void setUp() {
    AISpeechBubbleManager.clear();
    AISpeechBubbleManager.setTimeSourceForTest(clock::get);
    AISpeechBubbleManager.setTtlMillisForTest(5_000L);
  }

  @AfterEach
  void tearDown() {
    AISpeechBubbleManager.clear();
    AISpeechBubbleManager.setTimeSourceForTest(null);
    AISpeechBubbleManager.setTtlMillisForTest(AISpeechBubbleManager.DEFAULT_TTL_MS);
  }

  @Test
  void publish_storesBubble() {
    UUID id = UUID.randomUUID();
    AISpeechBubbleManager.publish(id, "hello");
    AISpeechBubbleManager.Bubble b = AISpeechBubbleManager.getActive(id, 0L);
    assertNotNull(b);
    assertEquals("hello", b.content());
  }

  @Test
  void publish_overwritesPrevious() {
    UUID id = UUID.randomUUID();
    AISpeechBubbleManager.publish(id, "first");
    clock.set(100L);
    AISpeechBubbleManager.publish(id, "second");
    AISpeechBubbleManager.Bubble b = AISpeechBubbleManager.getActive(id, 100L);
    assertEquals("second", b.content());
    assertEquals(100L, b.createdAtMillis());
  }

  @Test
  void getActive_expiresAfterTtl() {
    UUID id = UUID.randomUUID();
    AISpeechBubbleManager.publish(id, "ping");
    assertNotNull(AISpeechBubbleManager.getActive(id, 1_000L));
    assertNull(AISpeechBubbleManager.getActive(id, 6_000L));
    assertEquals(0, AISpeechBubbleManager.size(), "expired bubble must be evicted");
  }

  @Test
  void publish_ignoresNullsAndEmpty() {
    AISpeechBubbleManager.publish(null, "x");
    AISpeechBubbleManager.publish(UUID.randomUUID(), null);
    AISpeechBubbleManager.publish(UUID.randomUUID(), "");
    assertEquals(0, AISpeechBubbleManager.size());
  }
}
