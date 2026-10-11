package dev.adminecraft;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class AdminCommands {
	private AdminCommands() {}

	private static boolean isAdmin(ServerCommandSource src) {
		if (!(src.getEntity() instanceof ServerPlayerEntity p)) return true; // console / command blocks
		return ADMINecraft.STORE.isAdmin(src.getServer(), p.getGameProfile());
	}

	private static void ok(ServerCommandSource src, String msg) {
		src.sendFeedback(() -> Text.literal(msg).formatted(Formatting.GREEN), false);
	}

	private static BanDuration parse(ServerCommandSource src, String id) {
		BanDuration d = BanDuration.byId(id);
		if (d == null) src.sendError(Text.literal("Unknown duration. Use: " + String.join(", ", BanDuration.ids())));
		return d;
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> root = literal("adminecraft");

		// --- elections ---
		root.then(literal("elect").then(argument("player", EntityArgumentType.player()).executes(ctx -> {
			ServerPlayerEntity starter = ctx.getSource().getPlayerOrThrow();
			ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
			String err = ADMINecraft.ELECTIONS.start(ctx.getSource().getServer(), starter.getName().getString(), target, false);
			if (err != null) ctx.getSource().sendError(Text.literal(err));
			return err == null ? 1 : 0;
		})));
		root.then(literal("impeach").then(argument("player", EntityArgumentType.player()).executes(ctx -> {
			ServerPlayerEntity starter = ctx.getSource().getPlayerOrThrow();
			ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
			String err = ADMINecraft.ELECTIONS.start(ctx.getSource().getServer(), starter.getName().getString(), target, true);
			if (err != null) ctx.getSource().sendError(Text.literal(err));
			return err == null ? 1 : 0;
		})));
		root.then(literal("vote")
			.then(literal("yes").executes(ctx -> vote(ctx.getSource(), true)))
			.then(literal("no").executes(ctx -> vote(ctx.getSource(), false))));
		root.then(literal("status").executes(ctx -> {
			MinecraftServer server = ctx.getSource().getServer();
			ElectionManager.Election e = ADMINecraft.ELECTIONS.current();
			ok(ctx.getSource(), e == null ? "No election running." : ElectionManager.describe(e, server));
			return 1;
		}));

		// --- ban sticks & bans (admins only) ---
		root.then(literal("banstick").requires(AdminCommands::isAdmin)
			.then(argument("duration", StringArgumentType.word())
				.suggests((c, b) -> CommandSource.suggestMatching(BanDuration.ids(), b))
				.executes(ctx -> {
					BanDuration d = parse(ctx.getSource(), StringArgumentType.getString(ctx, "duration"));
					if (d == null) return 0;
					ServerPlayerEntity p = ctx.getSource().getPlayerOrThrow();
					p.giveItemStack(BanStick.create(d));
					ok(ctx.getSource(), "Gave you a Ban Stick [" + d.label + "].");
					return 1;
				})));
		root.then(literal("ban").requires(AdminCommands::isAdmin)
			.then(argument("player", EntityArgumentType.player())
				.then(argument("duration", StringArgumentType.word())
					.suggests((c, b) -> CommandSource.suggestMatching(BanDuration.ids(), b))
					.executes(ctx -> {
						ServerCommandSource src = ctx.getSource();
						BanDuration d = parse(src, StringArgumentType.getString(ctx, "duration"));
						if (d == null) return 0;
						ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
						if (ADMINecraft.STORE.isAdmin(src.getServer(), target.getGameProfile())) {
							src.sendError(Text.literal("You can't ban an admin."));
							return 0;
						}
						ADMINecraft.ban(src.getServer(), target.getGameProfile(), src.getName(), d);
						return 1;
					}))));
		root.then(literal("unban").requires(AdminCommands::isAdmin)
			.then(argument("name", StringArgumentType.word())
				.suggests((c, b) -> CommandSource.suggestMatching(
					c.getSource().getServer().getPlayerManager().getUserBanList().getNames(), b))
				.executes(ctx -> {
					ServerCommandSource src = ctx.getSource();
					String name = StringArgumentType.getString(ctx, "name");
					var list = src.getServer().getPlayerManager().getUserBanList();
					Optional<GameProfile> profile = src.getServer().getUserCache().findByName(name);
					if (profile.isEmpty() || !list.contains(profile.get())) {
						src.sendError(Text.literal(name + " is not banned."));
						return 0;
					}
					list.remove(profile.get());
					ok(src, "Unbanned " + name + ".");
					return 1;
				})));

		// --- admin list / op overrides ---
		root.then(literal("admins").executes(ctx -> {
			Map<String, String> all = ADMINecraft.STORE.all();
			ok(ctx.getSource(), all.isEmpty() ? "No elected admins." : "Elected admins: " + String.join(", ", all.values()));
			return 1;
		}));
		root.then(literal("admin").requires(s -> s.hasPermissionLevel(3))
			.then(literal("add").then(argument("player", EntityArgumentType.player()).executes(ctx -> {
				ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "player");
				ADMINecraft.STORE.add(t.getGameProfile());
				ok(ctx.getSource(), t.getName().getString() + " is now an admin.");
				return 1;
			})))
			.then(literal("remove").then(argument("player", EntityArgumentType.player()).executes(ctx -> {
				ServerPlayerEntity t = EntityArgumentType.getPlayer(ctx, "player");
				boolean removed = ADMINecraft.STORE.remove(t.getUuid());
				ok(ctx.getSource(), removed ? t.getName().getString() + " is no longer an admin." : "Not an elected admin.");
				return removed ? 1 : 0;
			}))));

		LiteralCommandNode<ServerCommandSource> node = dispatcher.register(root);
		dispatcher.register(literal("ae").redirect(node));
	}

	private static int vote(ServerCommandSource src, boolean yes) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity p = src.getPlayerOrThrow();
		String err = ADMINecraft.ELECTIONS.vote(src.getServer(), p, yes);
		if (err != null) {
			src.sendError(Text.literal(err));
			return 0;
		}
		ok(src, "Voted " + (yes ? "YES" : "NO") + ".");
		return 1;
	}
}
