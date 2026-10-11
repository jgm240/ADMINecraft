package dev.adminecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** A chat channel only admins can see. Players can also switch their normal chat into it. */
public final class AdminChat {
	private static final Set<UUID> MODE = new HashSet<>();

	private AdminChat() {}

	public static boolean isOn(UUID id) {
		return MODE.contains(id);
	}

	/** @return the new state. */
	public static boolean toggle(UUID id) {
		if (!MODE.remove(id)) {
			MODE.add(id);
			return true;
		}
		return false;
	}

	public static void off(UUID id) {
		MODE.remove(id);
	}

	public static void send(MinecraftServer server, Text body) {
		Text msg = Text.literal("[Admin Chat] ").formatted(Formatting.RED, Formatting.BOLD)
			.append(body.copy().formatted(Formatting.YELLOW));
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (ADMINecraft.STORE.isAdmin(server, p.getGameProfile())) p.sendMessage(msg, false);
		}
		server.sendMessage(msg); // console / log
	}
}
