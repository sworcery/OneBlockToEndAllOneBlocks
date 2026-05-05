package com.oneblocktoendall.team;

import com.oneblocktoendall.data.OneBlockWorldState;
import com.oneblocktoendall.quest.PlayerProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class TeamManager {

    public static String createTeam(MinecraftServer server, ServerPlayerEntity player, String teamName) {
        OneBlockWorldState state = OneBlockWorldState.get(server);
        PlayerProgress progress = state.getProgress(player.getUuid());
        if (progress == null || !progress.isStarted()) return "You haven't started the challenge!";
        if (progress.getTeamId() != null) return "You're already in a team!";
        if (teamName == null || teamName.isBlank()) return "Team name cannot be empty!";
        if (teamName.length() > 20) teamName = teamName.substring(0, 20);

        UUID teamId = UUID.randomUUID();
        Team team = new Team(teamId, teamName, player.getUuid());
        state.addTeam(team);
        progress.setTeamId(teamId);
        state.markDirty();
        return "Team '" + teamName + "' created!";
    }

    public static String invitePlayer(MinecraftServer server, ServerPlayerEntity inviter, String targetName) {
        OneBlockWorldState state = OneBlockWorldState.get(server);
        PlayerProgress inviterProgress = state.getProgress(inviter.getUuid());
        if (inviterProgress == null || inviterProgress.getTeamId() == null) return "You're not in a team!";

        Team team = state.getTeam(inviterProgress.getTeamId());
        if (team == null) return "Team not found!";
        if (!team.getLeaderId().equals(inviter.getUuid())) return "Only the leader can invite!";

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
        if (target == null) return "Player not found or offline!";

        PlayerProgress targetProgress = state.getProgress(target.getUuid());
        if (targetProgress == null || !targetProgress.isStarted()) return "That player hasn't started!";
        if (targetProgress.getTeamId() != null) return "That player is already in a team!";

        team.addInvite(target.getUuid());
        state.markDirty();

        target.sendMessage(Text.literal("You've been invited to team '" + team.getTeamName() + "' by " + inviter.getName().getString())
                .formatted(Formatting.GOLD));
        target.sendMessage(Text.literal("Press J > Team to accept or decline.")
                .formatted(Formatting.GRAY));

        return "Invite sent to " + targetName + "!";
    }

    public static String acceptInvite(MinecraftServer server, ServerPlayerEntity player, String leaderName) {
        OneBlockWorldState state = OneBlockWorldState.get(server);
        PlayerProgress progress = state.getProgress(player.getUuid());
        if (progress == null) return "You haven't started!";
        if (progress.getTeamId() != null) return "You're already in a team!";

        // Find team by leader name
        for (Team team : state.getAllTeams()) {
            if (team.hasInvite(player.getUuid())) {
                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(team.getLeaderId());
                if (leader != null && leader.getName().getString().equals(leaderName)) {
                    team.removeInvite(player.getUuid());
                    team.addMember(player.getUuid());
                    progress.setTeamId(team.getTeamId());
                    state.markDirty();

                    // Notify team
                    for (UUID memberId : team.getMembers()) {
                        ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                        if (member != null) {
                            member.sendMessage(Text.literal(player.getName().getString() + " joined the team!")
                                    .formatted(Formatting.GREEN));
                        }
                    }
                    return "Joined team '" + team.getTeamName() + "'!";
                }
            }
        }
        return "No matching invite found!";
    }

    public static String declineInvite(MinecraftServer server, ServerPlayerEntity player, String leaderName) {
        OneBlockWorldState state = OneBlockWorldState.get(server);
        for (Team team : state.getAllTeams()) {
            if (team.hasInvite(player.getUuid())) {
                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(team.getLeaderId());
                if (leader != null && leader.getName().getString().equals(leaderName)) {
                    team.removeInvite(player.getUuid());
                    state.markDirty();
                    return "Invite declined.";
                }
            }
        }
        return "No matching invite found!";
    }

    public static String leaveTeam(MinecraftServer server, ServerPlayerEntity player) {
        OneBlockWorldState state = OneBlockWorldState.get(server);
        PlayerProgress progress = state.getProgress(player.getUuid());
        if (progress == null || progress.getTeamId() == null) return "You're not in a team!";

        Team team = state.getTeam(progress.getTeamId());
        if (team == null) { progress.setTeamId(null); return "Left team."; }

        team.removeMember(player.getUuid());
        progress.setTeamId(null);
        state.markDirty();

        // If leader leaves, promote next member or disband
        if (team.getLeaderId().equals(player.getUuid())) {
            if (team.getMembers().isEmpty()) {
                state.removeTeam(team.getTeamId());
            } else {
                // Auto-disband if no members left, otherwise the team persists leaderless
                // (could promote, but keeping it simple)
                state.removeTeam(team.getTeamId());
                for (UUID memberId : team.getMembers()) {
                    PlayerProgress mp = state.getProgress(memberId);
                    if (mp != null) mp.setTeamId(null);
                    ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                    if (member != null) {
                        member.sendMessage(Text.literal("Team disbanded — the leader left.")
                                .formatted(Formatting.YELLOW));
                    }
                }
            }
        }
        return "You left the team.";
    }

    public static String kickPlayer(MinecraftServer server, ServerPlayerEntity kicker, String targetName) {
        OneBlockWorldState state = OneBlockWorldState.get(server);
        PlayerProgress kickerProgress = state.getProgress(kicker.getUuid());
        if (kickerProgress == null || kickerProgress.getTeamId() == null) return "You're not in a team!";

        Team team = state.getTeam(kickerProgress.getTeamId());
        if (team == null) return "Team not found!";
        if (!team.getLeaderId().equals(kicker.getUuid())) return "Only the leader can kick!";

        ServerPlayerEntity target = server.getPlayerManager().getPlayer(targetName);
        if (target == null) return "Player not found!";
        if (!team.isMember(target.getUuid())) return "That player is not in your team!";

        team.removeMember(target.getUuid());
        PlayerProgress tp = state.getProgress(target.getUuid());
        if (tp != null) tp.setTeamId(null);
        state.markDirty();

        target.sendMessage(Text.literal("You were kicked from team '" + team.getTeamName() + "'.")
                .formatted(Formatting.RED));
        return "Kicked " + targetName + " from the team.";
    }
}
