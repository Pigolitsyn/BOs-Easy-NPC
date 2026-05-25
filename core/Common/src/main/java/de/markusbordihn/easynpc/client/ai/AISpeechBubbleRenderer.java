/*
 * Copyright 2025 Markus Bordihn
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

package de.markusbordihn.easynpc.client.ai;

import com.mojang.blaze3d.vertex.PoseStack;
import de.markusbordihn.easynpc.entity.LivingEntityManager;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Renders the active AI speech bubble above each known easy NPC as billboard text. */
public final class AISpeechBubbleRenderer {

  private static final int BG_COLOR = 0x80000000;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final float TEXT_SCALE = 0.025F;
  private static final float VERTICAL_OFFSET = 0.55F;
  private static final int MAX_LINE_WIDTH = 110;
  private static final int LINE_HEIGHT = 10;

  private AISpeechBubbleRenderer() {}

  public static void renderAll(PoseStack poseStack, Vec3 cameraPos, Quaternionf cameraRotation) {
    Minecraft mc = safeMinecraft();
    if (mc == null || mc.font == null || cameraPos == null || cameraRotation == null) {
      return;
    }
    MultiBufferSource.BufferSource bufferSource = bufferSource(mc);
    if (bufferSource == null) {
      return;
    }
    long now = System.currentTimeMillis();

    for (var entry : LivingEntityManager.getNpcEntityMap().entrySet()) {
      UUID id = entry.getKey();
      EasyNPC<?> easyNPC = entry.getValue();
      AISpeechBubbleManager.Bubble bubble = AISpeechBubbleManager.getActive(id, now);
      if (bubble == null || easyNPC == null || easyNPC.getLivingEntity() == null) {
        continue;
      }
      LivingEntity le = easyNPC.getLivingEntity();
      if (le.isRemoved() || le.level() != mc.level) {
        continue;
      }
      double dx = le.getX() - cameraPos.x;
      double dy = le.getY() + le.getBbHeight() + VERTICAL_OFFSET - cameraPos.y;
      double dz = le.getZ() - cameraPos.z;
      if (dx * dx + dy * dy + dz * dz > 64.0 * 64.0) {
        continue;
      }
      renderBubble(poseStack, bufferSource, cameraRotation, mc.font, dx, dy, dz, bubble.content());
    }
    bufferSource.endBatch();
  }

  private static void renderBubble(
      PoseStack poseStack,
      MultiBufferSource bufferSource,
      Quaternionf cameraRotation,
      Font font,
      double dx,
      double dy,
      double dz,
      String content) {
    List<String> lines = wrap(font, content, MAX_LINE_WIDTH);
    if (lines.isEmpty()) {
      return;
    }

    poseStack.pushPose();
    poseStack.translate(dx, dy, dz);
    poseStack.mulPose(new Quaternionf(cameraRotation));
    poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

    var matrix = poseStack.last().pose();
    int packedLight = 0xF000F0;
    int y = -LINE_HEIGHT * lines.size();
    for (String line : lines) {
      float lx = -font.width(line) / 2.0f;
      font.drawInBatch(
          line,
          lx,
          y,
          TEXT_COLOR,
          false,
          matrix,
          bufferSource,
          Font.DisplayMode.SEE_THROUGH,
          BG_COLOR,
          packedLight);
      y += LINE_HEIGHT;
    }
    poseStack.popPose();
  }

  static List<String> wrap(Font font, String text, int maxWidth) {
    List<String> out = new ArrayList<>();
    if (text == null || text.isEmpty()) {
      return out;
    }
    String remaining = text;
    while (!remaining.isEmpty()) {
      String sub = font.plainSubstrByWidth(remaining, maxWidth);
      if (sub.isEmpty()) {
        sub = remaining.substring(0, 1);
      }
      out.add(sub);
      remaining = remaining.substring(sub.length());
      if (out.size() >= 4) {
        if (!remaining.isEmpty()) {
          out.set(3, out.get(3) + "...");
        }
        break;
      }
    }
    return out;
  }

  private static Minecraft safeMinecraft() {
    try {
      return Minecraft.getInstance();
    } catch (Throwable t) {
      return null;
    }
  }

  private static MultiBufferSource.BufferSource bufferSource(Minecraft mc) {
    RenderBuffers buffers = mc.renderBuffers();
    return buffers == null ? null : buffers.bufferSource();
  }
}
