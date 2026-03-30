package com.oneblocktoendall.network;

import com.oneblocktoendall.phase.Phase;
import com.oneblocktoendall.phase.PhaseManager;
import com.oneblocktoendall.quest.PlayerProgress;
import com.oneblocktoendall.quest.Quest;
import com.oneblocktoendall.quest.QuestManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers network payloads and provides helper methods for sending packets.
 *
 * The mod uses a simple one-way sync: server -> client.
 * The server sends QuestSyncPayload whenever quest progress changes,
 * and the client caches it for HUD/GUI rendering.
 */
public class ModNetworking {

    /**
     * Register server-to-client (S2C) packet types.
     * Must be called during mod initialization (before any packets are sent).
     */
    public static void registerS2CPackets() {
        PayloadTypeRegistry.playS2C().register(QuestSyncPayload.ID, QuestSyncPayload.CODEC);
    }

    /**
     * Send current quest progress to a specific player's client.
     */
    public static void syncQuestProgress(ServerPlayerEntity player, PlayerProgress progress) {
        Phase phase = PhaseManager.getPhase(progress.getCurrentPhase());
        if (phase == null) return;

        List<QuestSyncPayload.QuestStatus> questStatuses = new ArrayList<>();
        for (Quest quest : phase.quests()) {
            questStatuses.add(new QuestSyncPayload.QuestStatus(
                    quest.id(),
                    quest.name(),
                    quest.description(),
                    QuestManager.getQuestProgress(player, quest, progress),
                    quest.count(),
                    progress.isQuestCompleted(quest.id())
            ));
        }

        QuestSyncPayload payload = new QuestSyncPayload(
                progress.getCurrentPhase(),
                PhaseManager.getMaxPhase(),
                phase.name(),
                questStatuses
        );

        ServerPlayNetworking.send(player, payload);
    }
}
