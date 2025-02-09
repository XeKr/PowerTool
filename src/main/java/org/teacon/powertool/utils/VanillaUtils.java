package org.teacon.powertool.utils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import org.teacon.powertool.PowerTool;
import org.teacon.powertool.client.overlay.ClientDebugCharts;
import org.teacon.powertool.network.client.RecordDebugData;

import java.util.UUID;

public class VanillaUtils {
    
    public static final Direction[] DIRECTIONS = Direction.values();
    
    public static ResourceLocation modRL(String path) {
        return resourceLocationOf(PowerTool.MODID, path);
    }
    
    public static ResourceLocation resourceLocationOf(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
    
    public static EquipmentSlot equipmentSlotFromHand(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }
    
    public static ItemInteractionResult itemInteractionFrom(InteractionResult result) {
        return switch (result){
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }
    
    public static void runCommand(String command, LivingEntity livingEntity) {
        // Raise permission level to 2, akin to what vanilla sign does
        CommandSourceStack cmdSrc = livingEntity.createCommandSourceStack().withPermission(2);
        var server = livingEntity.level().getServer();
        if (server != null) {
            server.getCommands().performPrefixedCommand(cmdSrc, command);
        }
    }
    
    public static void runCommand(String command,MinecraftServer server, UUID playerUUID){
        var player = server.getPlayerList().getPlayer(playerUUID);
        if(player != null){
            server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2),command);
        }
    }
    
    //irrelevant vanilla(笑)
    public static int getColor(int r,int g,int b,int a){
        return a << 24 | r << 16 | g << 8 | b;
    }
    
    public static int parseColorHEX(String color) throws IllegalArgumentException {
        if(color.length() == 6){
            return getColor(
                    Integer.parseInt(color.substring(0,2),16),
                    Integer.parseInt(color.substring(2,4),16),
                    Integer.parseInt(color.substring(4,6),16),
                    255);
        }
        if(color.length() == 8){
            return getColor(
                    Integer.parseInt(color.substring(0,2),16),
                    Integer.parseInt(color.substring(2,4),16),
                    Integer.parseInt(color.substring(4,6),16),
                    Integer.parseInt(color.substring(6,8),16)
            );
        }
        throw new IllegalArgumentException("Format of color must be RGB or RGBA digits");
    }
    
    public static String hexColorFromInt(int color){
        var a = color >>> 24;
        var r = (color >> 16) & 0xFF;
        var g = (color >> 8) & 0xFF;
        var b = color & 0xFF;
        return String.format("%02X%02X%02X%02X", r, g, b, a).toUpperCase();
    }
    
    @SuppressWarnings("SuspiciousNameCombination")
    public static Vec2 rotate90FromBlockCenterYP(Vec2 point, int times) {
        times = times % 4;
        if (times == 0) return point;
        var x = point.x;
        var y = point.y;
        if (times == 1) return new Vec2(16-y,x);
        if (times == 2) return new Vec2(16-x,16-y);
        return new Vec2(y,16-x);
    }

    public static Component getName(Block block) {
        ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(block);
        return Component.translatable("block." + rl.getNamespace() + "." + rl.getPath());
    }
    
    public static void recordDebugData(String id,long data){
        if(FMLEnvironment.dist == Dist.DEDICATED_SERVER){
            PacketDistributor.sendToAllPlayers(new RecordDebugData(id, data));
        }
        else {
            ClientHandler.handleDebugData(id, data);
        }
    }
    
    public static class ClientHandler{
        public static void handleDebugData(String id,long data){
            ClientDebugCharts.recordDebugData(id,data);
        }
    }
}
