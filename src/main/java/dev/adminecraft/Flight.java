package dev.adminecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Lets admins fly in Survival/Adventure. Lasts until restart; revoked automatically if they stop being admin. */
public final class Flight {
	private static final Set<UUID> ENABLED = new HashSet<>();

	private Flight() {}

	public static boolean isEnabled(UUID id) {
		return ENABLED.contains(id);
	}

	public static void toggle(MinecraftServer server, ServerPlayerEntity p) {
		if (!ENABLED.remove(p.getUuid())) ENABLED.add(p.getUuid());
		apply(server, p);
	}

	/** Re-sync the player's abilities with their flight setting (call after gamemode change, join, respawn). */
	public static void apply(MinecraftServer server, ServerPlayerEntity p) {
		GameMode gm = p.interactionManager.getGameMode();
		if (gm != GameMode.SURVIVAL && gm != GameMode.ADVENTURE) return; // creative/spectator manage this themselves
		boolean want = ENABLED.contains(p.getUuid()) && ADMINecraft.STORE.isAdmin(server, p.getGameProfile());
		p.getAbilities().allowFlying = want;
		if (!want) p.getAbilities().flying = false;
		p.sendAbilitiesUpdate();
	}
}
