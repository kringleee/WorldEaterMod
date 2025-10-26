package net.kringle.worldeater;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import java.util.Random;

public class BlockDestroyer implements ModInitializer {

    private final Random random = new Random();
    private boolean safeModeEnabled = false;
    private boolean chaoticModeEnabled = false;

    private int safeStrength = 1;  // blocks per tick for safe mode
    private int chaosStrength = 5; // blocks per tick for chaotic mode

    @Override
    public void onInitialize() {
        // Server tick event
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                BlockPos playerPos = player.getBlockPos();
                World world = player.getEntityWorld();

                // Safe mode
                if (safeModeEnabled) {
                    for (int i = 0; i < safeStrength; i++) {
                        if (random.nextInt(20) == 0) { // roughly 1 block per second per strength
                            destroyRandomBlock(world, playerPos, 10, new Block[]{Blocks.STONE, Blocks.DIRT, Blocks.GRASS_BLOCK});
                        }
                    }
                }

                // Chaotic mode
                if (chaoticModeEnabled) {
                    for (int i = 0; i < chaosStrength; i++) {
                        destroyRandomBlock(world, playerPos, 20, new Block[]{
                                Blocks.STONE, Blocks.DIRT, Blocks.OAK_LOG, Blocks.SAND, Blocks.WATER,
                                Blocks.GRAVEL, Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.GRASS_BLOCK
                        });
                    }
                }
            }
        });

        // Commands with optional strength argument
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("safeblocks")
                    .then(CommandManager.argument("strength", IntegerArgumentType.integer(1, 1000))
                            .executes(context -> {
                                safeStrength = IntegerArgumentType.getInteger(context, "strength");
                                safeModeEnabled = true;
                                context.getSource().sendFeedback(() ->
                                        Text.literal("Safe Blocks Mode Enabled With Strength " + safeStrength), false);
                                return 1;
                            })
                    )
                    .executes(context -> {
                        safeModeEnabled = !safeModeEnabled;
                        context.getSource().sendFeedback(() ->
                                Text.literal("Safe Blocks Mode " + (safeModeEnabled ? "Enabled" : "Disabled")), false);
                        return 1;
                    })
            );

            dispatcher.register(CommandManager.literal("chaosblocks")
                    .then(CommandManager.argument("strength", IntegerArgumentType.integer(1, 1000))
                            .executes(context -> {
                                chaosStrength = IntegerArgumentType.getInteger(context, "strength");
                                chaoticModeEnabled = true;
                                context.getSource().sendFeedback(() ->
                                        Text.literal("Chaos Blocks Mode Enabled With Strength " + chaosStrength), false);
                                return 1;
                            })
                    )
                    .executes(context -> {
                        chaoticModeEnabled = !chaoticModeEnabled;
                        context.getSource().sendFeedback(() ->
                                Text.literal("Chaos Blocks Mode " + (chaoticModeEnabled ? "Enabled" : "Disabled")), false);
                        return 1;
                    })
            );
        });
    }

    // Destroy a random block in radius around player
    private void destroyRandomBlock(World world, BlockPos playerPos, int radius, Block[] allowedBlocks) {
        int dx = random.nextInt(radius * 2 + 1) - radius;
        int dy = random.nextInt(radius * 2 + 1) - radius;
        int dz = random.nextInt(radius * 2 + 1) - radius;

        BlockPos targetPos = playerPos.add(dx, dy, dz);
        Block targetBlock = world.getBlockState(targetPos).getBlock();

        for (Block block : allowedBlocks) {
            if (targetBlock == block) {
                world.setBlockState(targetPos, Blocks.AIR.getDefaultState());
                break;
            }
        }
    }
}
