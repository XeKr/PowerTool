package org.teacon.powertool;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.teacon.powertool.attachment.PowerToolAttachments;
import org.teacon.powertool.entity.AutoVanishBoat;
import org.teacon.powertool.entity.AutoVanishMinecart;
import org.teacon.powertool.network.client.UpdateDisplayChunkDataPacket;
import org.teacon.powertool.utils.DelayServerExecutor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = PowerTool.MODID)
public class PowerToolEvents {

    @SubscribeEvent
    public static void on(ChunkWatchEvent.Sent event) {
        ChunkPos chunkPos = event.getPos();
        List<BlockPos> list = event.getChunk().getData(PowerToolAttachments.DISPLAY_MODE);
        PacketDistributor.sendToPlayer(
            event.getPlayer(),
            new UpdateDisplayChunkDataPacket(
                chunkPos.x,
                chunkPos.z,
                list
            )
        );
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Map<ChunkPos, List<BlockPos>> map = new HashMap<>();
        for (BlockPos affectedBlock : event.getAffectedBlocks()) {
            map.computeIfAbsent(new ChunkPos(affectedBlock), it -> new ArrayList<>())
                .add(affectedBlock);
        }
        map.entrySet()
            .stream()
            .map(it -> Map.entry(
                Map.entry(
                    event.getLevel().getChunk(it.getKey().x, it.getKey().z),
                    it.getKey()
                ),
                it.getValue()
            ))
            .collect(Util.toMap())
            .forEach((entry, list) -> {
                LevelChunk chunk = entry.getKey();
                ChunkPos pos = entry.getValue();
                for (BlockPos blockPos : list) {
                    removeDisplayMode(
                        (ServerLevel) event.getLevel(),
                        pos,
                        chunk,
                        blockPos,
                        null
                    );
                }
            });
    }

    private static void removeDisplayMode(
        ServerLevel level,
        ChunkPos chunkPos,
        ChunkAccess chunk,
        BlockPos pos,
        @Nullable ServerPlayer player
    ) {
        List<BlockPos> displayEnabledPosList = new ArrayList<>(chunk.getData(PowerToolAttachments.DISPLAY_MODE));
        displayEnabledPosList.remove(pos);
        chunk.setUnsaved(true);
        chunk.setData(PowerToolAttachments.DISPLAY_MODE, displayEnabledPosList);
        UpdateDisplayChunkDataPacket data = new UpdateDisplayChunkDataPacket(
            chunkPos.x,
            chunkPos.z,
            displayEnabledPosList
        );
        if (player == null) {
            PacketDistributor.sendToPlayersTrackingChunk(
                level,
                chunkPos,
                data
            );
            return;
        }
        PacketDistributor.sendToPlayer(
            player,
            data
        );
    }

    private static void removeDisplayMode(
        ServerLevel level,
        BlockPos pos,
        @Nullable ServerPlayer player
    ) {
        ChunkPos chunkPos = new ChunkPos(pos);
        ChunkAccess chunk = level.getChunk(pos);
        removeDisplayMode(
            level,
            chunkPos,
            chunk,
            pos,
            player
        );
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        removeDisplayMode(
            level,
            pos,
            (ServerPlayer) event.getPlayer()
        );
    }
    
    @SubscribeEvent
    public static void onChangeDimension(EntityTravelToDimensionEvent event) {
        if(event.getDimension().equals(ServerLevel.END) && PowerToolConfig.disableTeleportToEnd.get()){
            event.setCanceled(true);
        }
    }
    
    @SubscribeEvent
    public static void onAddEntity(EntityJoinLevelEvent event){
        var entity = event.getEntity();
        var level = event.getLevel();
        if(PowerToolConfig.vehicleAutoVanish.get()){
            if(entity instanceof Boat boat && !(entity instanceof ChestBoat) &&!(entity instanceof AutoVanishBoat)){
                var newBoat = AutoVanishBoat.fromBoat(boat);
                DelayServerExecutor.addTask(2,(server) -> level.addFreshEntity(newBoat));
                event.setCanceled(true);
            }
            if(entity instanceof Minecart minecart && !(entity instanceof AutoVanishMinecart)){
                var newMinecart = AutoVanishMinecart.fromMinecart(minecart);
                DelayServerExecutor.addTask(2,(server) -> level.addFreshEntity(newMinecart));
                event.setCanceled(true);
            }
        }
    }
}
