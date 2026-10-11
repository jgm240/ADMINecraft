package dev.adminecraft;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** One election at a time: either "make X an admin" or "impeach admin X". */
public final class ElectionManager {
	public static final int DURATION_TICKS = 20 * 60 * 3; // 3 minutes

	public static final class Election {
		public final UUID target;
		public final String targetName;
		public final boolean impeach;
		public final String starter;
		public final long endTick;
		public final Set<UUID> yes = new HashSet<>();
		public final Set<UUID> no = new HashSet<>();

		Election(UUID target, String targetName, boolean impeach, String starter, long endTick) {
			this.target = target;
			this.targetName = targetName;
			this.impeach = impeach;
			this.starter = starter;
			this.endTick = endTick;
		}
	}

	private Election current;

	public Election current() {
		return current;
	}

	/** @return an error message, or null on success. */
	public String start(MinecraftServer server, String starter, ServerPlayerEntity target, boolean impeach) {
		if (current != null) return "An election for " + current.targetName + " is already running.";
		AdminStore store = ADMINecraft.STORE;
		if (impeach) {
			if (!store.isElected(target.getUuid())) return target.getName().getString() + " is not an elected admin.";
		} else if (store.isAdmin(server, target.getGameProfile())) {
			return target.getName().getString() + " is already an admin.";
		}
		current = new Election(target.getUuid(), target.getName().getString(), impeach, starter,
			server.getTicks() + DURATION_TICKS);
		server.getPlayerManager().broadcast(Text.literal(
			"[ADMINecraft] " + starter + " started an election to " + (impeach ? "REMOVE " : "ELECT ")
				+ current.targetName + " as admin" + ". Vote with /ae vote yes|no (3 minutes)")
			.formatted(Formatting.GOLD), false);
		return null;
	}

	/** @return an error message, or null on success. */
	public String vote(MinecraftServer server, ServerPlayerEntity voter, boolean yes) {
		if (current == null) return "There is no election running.";
		if (voter.getUuid().equals(current.target)) return "You can't vote in your own election.";
		current.yes.remove(voter.getUuid());
		current.no.remove(voter.getUuid());
		(yes ? current.yes : current.no).add(voter.getUuid());
		if (current.yes.size() + current.no.size() >= eligible(server)) resolve(server);
		return null;
	}

	private int eligible(MinecraftServer server) {
		int n = 0;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (!p.getUuid().equals(current.target)) n++;
		}
		return n;
	}

	public void tick(MinecraftServer server) {
		if (current != null && server.getTicks() >= current.endTick) resolve(server);
	}

	private void resolve(MinecraftServer server) {
		Election e = current;
		current = null;
		int yes = e.yes.size(), no = e.no.size();
		int quorum = Math.max(1, (eligibleAtClose(server, e) + 1) / 2);
		boolean passed = yes > no && yes + no >= quorum;
		String result = " (" + yes + " yes / " + no + " no)";
		if (passed) {
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(e.target);
			if (e.impeach) {
				ADMINecraft.STORE.remove(e.target);
			} else if (online != null) {
				ADMINecraft.STORE.add(online.getGameProfile());
			}
			server.getPlayerManager().broadcast(Text.literal("[ADMINecraft] " + e.targetName
				+ (e.impeach ? " has been removed as admin" : " is now an admin!") + result)
				.formatted(Formatting.GREEN), false);
			if (online != null) {
				server.getPlayerManager().sendCommandTree(online);
				Flight.apply(server, online);
			}
		} else {
			server.getPlayerManager().broadcast(Text.literal("[ADMINecraft] The election for " + e.targetName
				+ " failed" + result + ". Needs more yes than no and at least " + quorum + " vote(s).")
				.formatted(Formatting.RED), false);
		}
	}

	private int eligibleAtClose(MinecraftServer server, Election e) {
		int n = 0;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (!p.getUuid().equals(e.target)) n++;
		}
		return n;
	}

	public static String describe(Election e, MinecraftServer server) {
		long secs = Math.max(0, (e.endTick - server.getTicks()) / 20);
		return (e.impeach ? "Impeach " : "Elect ") + e.targetName + " — " + e.yes.size() + " yes / "
			+ e.no.size() + " no, " + secs + "s left";
	}
}
