package dev.adminecraft;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.BannedPlayerEntry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class ADMINecraft implements ModInitializer {
	public static final String MOD_ID = "adminecraft";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final AdminStore STORE = new AdminStore();
	public static final ElectionManager ELECTIONS = new ElectionManager();

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(ActionPayload.ID, ActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ActionPayload.ID, (payload, ctx) ->
			ActionHandler.handle(ctx.player().getServer(), ctx.player(), payload));
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Flight.apply(server, handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
			Flight.apply(newPlayer.getServer(), newPlayer));

		ModEntities.register();
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			STORE.load(server.getSavePath(WorldSavePath.ROOT).resolve("adminecraft_admins.json"));
			DebugPlayerEntity.loadCounter(server.getSavePath(WorldSavePath.ROOT).resolve("adminecraft_debug_counter.txt"));
		});
		ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
			if (!AdminChat.isOn(sender.getUuid())) return true;
			MinecraftServer server = sender.getServer();
			if (!STORE.isAdmin(server, sender.getGameProfile())) {
				AdminChat.off(sender.getUuid());
				return true;
			}
			AdminChat.send(server, Text.literal("<" + sender.getName().getString() + "> ").append(message.getContent()));
			return false; // keep it out of public chat
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> STORE.save());
		ServerTickEvents.END_SERVER_TICK.register(ELECTIONS::tick);
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> AdminCommands.register(dispatcher));

		// Punching a player with a Ban Stick bans them.
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClient || !(player instanceof ServerPlayerEntity admin)) return ActionResult.PASS;
			BanDuration duration = BanStick.read(admin.getStackInHand(hand));
			if (duration == null) return ActionResult.PASS;
			if (!(entity instanceof ServerPlayerEntity) && !(entity instanceof DebugPlayerEntity)) return ActionResult.PASS;

			MinecraftServer server = admin.getServer();
			if (!STORE.isAdmin(server, admin.getGameProfile())) {
				admin.sendMessage(Text.literal("Only admins can use a Ban Stick.").formatted(Formatting.RED), true);
				return ActionResult.FAIL;
			}
			if (entity instanceof DebugPlayerEntity dummy) {
				AdminChat.send(server, Text.literal("Debug player #" + dummy.getNumber() + " got banned for " + duration.label));
				dummy.discard();
				return ActionResult.FAIL;
			}
			ServerPlayerEntity target = (ServerPlayerEntity) entity;
			if (STORE.isAdmin(server, target.getGameProfile())) {
				admin.sendMessage(Text.literal("You can't ban another admin.").formatted(Formatting.RED), true);
				return ActionResult.FAIL;
			}
			ban(server, target.getGameProfile(), admin.getName().getString(), duration);
			return ActionResult.FAIL; // cancel the normal hit
		});
	}

	public static void ban(MinecraftServer server, GameProfile profile, String by, BanDuration d) {
		Date now = new Date();
		Date expires = d.isPermanent() ? null : new Date(now.getTime() + d.millis);
		String reason = "Banned by " + by + " (" + d.label + ")";
		server.getPlayerManager().getUserBanList().add(new BannedPlayerEntry(profile, now, by, expires, reason));

		ServerPlayerEntity online = server.getPlayerManager().getPlayer(profile.getId());
		if (online != null) {
			online.networkHandler.disconnect(Text.literal(reason));
		}
		server.getPlayerManager().broadcast(Text.literal("[ADMINecraft] " + profile.getName() + " was banned by "
			+ by + " — " + d.label).formatted(Formatting.RED), false);
	}
}
