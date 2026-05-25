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

package de.markusbordihn.easynpc.server.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import de.markusbordihn.easynpc.commands.Command;
import de.markusbordihn.easynpc.data.display.NameVisibilityType;
import de.markusbordihn.easynpc.entity.LivingEntityManager;
import de.markusbordihn.easynpc.entity.ModCustomEntityType;
import de.markusbordihn.easynpc.entity.ModEntityTypeProvider;
import de.markusbordihn.easynpc.entity.ModNPCEntityType;
import de.markusbordihn.easynpc.entity.ModRawEntityType;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import de.markusbordihn.easynpc.handler.NameHandler;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class AddCommand extends Command {

  private static final String NAME_ARG = "name";

  private AddCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    var typeArg =
        Commands.argument(TYPE_ARG, StringArgumentType.word())
            .suggests(
                (context, builder) -> {
                  for (ModNPCEntityType type : ModNPCEntityType.values()) {
                    builder.suggest(type.name().toLowerCase(Locale.ROOT));
                  }
                  for (ModRawEntityType type : ModRawEntityType.values()) {
                    builder.suggest(type.name().toLowerCase(Locale.ROOT));
                  }
                  for (ModCustomEntityType type : ModCustomEntityType.values()) {
                    builder.suggest(type.name().toLowerCase(Locale.ROOT));
                  }
                  return builder.buildFuture();
                });

    var posWithName =
        Commands.argument(POSITION_ARG, Vec3Argument.vec3())
            .executes(
                context ->
                    addNPC(
                        context.getSource(),
                        StringArgumentType.getString(context, TYPE_ARG),
                        Vec3Argument.getVec3(context, POSITION_ARG),
                        null))
            .then(
                Commands.argument(NAME_ARG, StringArgumentType.greedyString())
                    .executes(
                        context ->
                            addNPC(
                                context.getSource(),
                                StringArgumentType.getString(context, TYPE_ARG),
                                Vec3Argument.getVec3(context, POSITION_ARG),
                                StringArgumentType.getString(context, NAME_ARG))));

    return Commands.literal("add")
        .requires(cs -> cs.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
        .then(typeArg.then(posWithName));
  }

  private static int addNPC(
      CommandSourceStack context, String typeName, Vec3 position, String name) {
    ModEntityTypeProvider npcType = null;
    String input = typeName.toUpperCase(Locale.ROOT);

    try {
      npcType = ModNPCEntityType.valueOf(input);
    } catch (IllegalArgumentException e) {
      try {
        npcType = ModRawEntityType.valueOf(input);
      } catch (IllegalArgumentException e2) {
        try {
          npcType = ModCustomEntityType.valueOf(input);
        } catch (IllegalArgumentException e3) {
          // Ignore
        }
      }
    }

    if (npcType == null) {
      return sendFailureMessage(context, "Invalid NPC type: " + typeName);
    }

    ServerLevel serverLevel = context.getLevel();
    EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(npcType.getResourceKey());
    if (entityType == null) {
      return sendFailureMessage(context, "Could not find entity type for: " + npcType);
    }

    BlockPos blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
    net.minecraft.world.entity.Entity entity =
        entityType.spawn(serverLevel, null, null, blockPos, EntitySpawnReason.COMMAND, true, false);

    if (entity == null) {
      return sendFailureMessage(context, "Failed to create " + typeName + " NPC at " + blockPos);
    }

    if (name != null && !name.isBlank()) {
      EasyNPC<?> easyNPC = LivingEntityManager.getEasyNPCEntityByUUID(entity.getUUID(), serverLevel);
      if (easyNPC != null) {
        NameHandler.setCustomName(easyNPC, name, -1, NameVisibilityType.ALWAYS);
      }
    }

    return sendSuccessMessage(context, entity.getUUID().toString());
  }
}
