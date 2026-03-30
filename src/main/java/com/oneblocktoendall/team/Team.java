package com.oneblocktoendall.team;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.*;

public class Team {

    private UUID teamId;
    private String teamName;
    private UUID leaderId;
    private final Set<UUID> members = new HashSet<>();
    private final Set<UUID> pendingInvites = new HashSet<>();
    private boolean mergedIslands = false;

    public Team(UUID teamId, String teamName, UUID leaderId) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.leaderId = leaderId;
        this.members.add(leaderId);
    }

    public UUID getTeamId() { return teamId; }
    public String getTeamName() { return teamName; }
    public UUID getLeaderId() { return leaderId; }
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public Set<UUID> getPendingInvites() { return Collections.unmodifiableSet(pendingInvites); }
    public boolean isMergedIslands() { return mergedIslands; }
    public void setMergedIslands(boolean merged) { this.mergedIslands = merged; }

    public void addMember(UUID playerId) { members.add(playerId); }
    public void removeMember(UUID playerId) { members.remove(playerId); }
    public boolean isMember(UUID playerId) { return members.contains(playerId); }
    public void addInvite(UUID playerId) { pendingInvites.add(playerId); }
    public void removeInvite(UUID playerId) { pendingInvites.remove(playerId); }
    public boolean hasInvite(UUID playerId) { return pendingInvites.contains(playerId); }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("teamId", teamId);
        nbt.putString("teamName", teamName);
        nbt.putUuid("leaderId", leaderId);
        nbt.putBoolean("mergedIslands", mergedIslands);

        NbtList memberList = new NbtList();
        for (UUID id : members) memberList.add(NbtString.of(id.toString()));
        nbt.put("members", memberList);

        NbtList inviteList = new NbtList();
        for (UUID id : pendingInvites) inviteList.add(NbtString.of(id.toString()));
        nbt.put("pendingInvites", inviteList);

        return nbt;
    }

    public static Team fromNbt(NbtCompound nbt) {
        Team team = new Team(
                nbt.getUuid("teamId"),
                nbt.getString("teamName"),
                nbt.getUuid("leaderId")
        );
        team.mergedIslands = nbt.getBoolean("mergedIslands");
        team.members.clear();
        NbtList memberList = nbt.getList("members", 8); // 8 = NbtString
        for (int i = 0; i < memberList.size(); i++) {
            team.members.add(UUID.fromString(memberList.getString(i)));
        }
        NbtList inviteList = nbt.getList("pendingInvites", 8);
        for (int i = 0; i < inviteList.size(); i++) {
            team.pendingInvites.add(UUID.fromString(inviteList.getString(i)));
        }
        return team;
    }
}
