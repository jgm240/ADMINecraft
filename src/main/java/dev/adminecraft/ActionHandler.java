package dev.adminecraft;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

/** Server-side handling of GUI button presses. Never trust the client: every action is re-checked here. */
public final class ActionHandler {
	private ActionHandler() {}

	public static void handle(MinecraftServer server, ServerPlayerEntity p, ActionPayload payload) {
		boolean admin = ADMINecraft.STORE.isAdmin(server, p.getGameProfile());
		String arg = payload.arg();

		switch (payload.action()) {
			case "open", "refresh" -> { }
			case "elect", "impeach" -> {
				ServerPlayerEntity target = server.getPlayerManager().getPlayerList().stream()
					.filter(o -> o.getName().getString().equals(arg)).findFirst().orElse(null);
				if (target == null || target == p) {
					msg(p, "Pick another online player first.", Formatting.RED);
				} else {
					String err = ADMINecraft.ELECTIONS.start(server, p.getName().getString(), target,
						payload.action().equals("impeach"));
					if (err != null) msg(p, err, Formatting.RED);
				}
			}
			case "vote" -> {
				String err = ADMINecraft.ELECTIONS.vote(server, p, arg.equals("yes"));
				msg(p, err != null ? err : "Voted " + arg.toUpperCase() + ".", err != null ? Formatting.RED : Formatting.GREEN);
			}
			case "gamemode" -> {
				GameMode gm = GameMode.byName(arg, null);
				if (!admin) deny(p);
				else if (gm == null) msg(p, "Unknown gamemode.", Formatting.RED);
				else {
					p.changeGameMode(gm);
					Flight.apply(server, p);
				}
			}
			case "fly" -> {
				if (!admin) deny(p);
				else Flight.toggle(server, p);
			}
			case "banstick" -> {
				BanDuration d = BanDuration.byId(arg);
				if (!admin) deny(p);
				else if (d == null) msg(p, "Unknown duration.", Formatting.RED);
				else p.giveItemStack(BanStick.create(d));
			}
			case "enchant" -> {
				if (!admin) deny(p);
				else enchantHeld(server, p, arg);
			}
			default -> { }
		}
		sendState(server, p, payload.action().equals("open"));
	}

	private static void enchantHeld(MinecraftServer server, ServerPlayerEntity p, String id) {
		ItemStack stack = p.getMainHandStack();
		if (BanStick.read(stack) == null) {
			msg(p, "Hold a Ban Stick in your main hand first.", Formatting.RED);
			return;
		}
		List<RegistryKey<Enchantment>> keys = new ArrayList<>();
		if (id.equals("all")) keys.addAll(StickEnchants.ALL.values());
		else if (StickEnchants.ALL.containsKey(id)) keys.add(StickEnchants.ALL.get(id));
		else {
			msg(p, "Unknown enchantment.", Formatting.RED);
			return;
		}
		Registry<Enchantment> registry = server.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
		for (RegistryKey<Enchantment> key : keys) {
			stack.addEnchantment(registry.getOrThrow(key), StickEnchants.LEVEL);
		}
		msg(p, "Enchanted your Ban Stick (level " + StickEnchants.LEVEL + ").", Formatting.GREEN);
	}

	public static void sendState(MinecraftServer server, ServerPlayerEntity p, boolean open) {
		List<String> players = new ArrayList<>();
		for (ServerPlayerEntity o : server.getPlayerManager().getPlayerList()) {
			if (o != p) players.add(o.getName().getString());
		}
		ElectionManager.Election e = ADMINecraft.ELECTIONS.current();
		String election = e == null ? "" : ElectionManager.describe(e, server);
		ServerPlayNetworking.send(p, new StatePayload(open, ADMINecraft.STORE.isAdmin(server, p.getGameProfile()),
			Flight.isEnabled(p.getUuid()), election, players));
	}

	private static void deny(ServerPlayerEntity p) {
		msg(p, "Admins only.", Formatting.RED);
	}

	private static void msg(ServerPlayerEntity p, String s, Formatting color) {
		p.sendMessage(Text.literal("[ADMINecraft] " + s).formatted(color), false);
	}
}
