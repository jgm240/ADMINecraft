package dev.adminecraft;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class AdminScreen extends Screen {
	private static final int PAGE_VOTING = 0, PAGE_TOOLS = 1, PAGE_ENCHANTS = 2;

	private StatePayload state;
	private int page = PAGE_VOTING;
	private String selected;
	private int ticks;

	public AdminScreen(StatePayload state) {
		super(Text.literal("ADMINecraft"));
		this.state = state;
	}

	public void update(StatePayload next) {
		boolean rebuild = next.admin() != state.admin() || next.canFly() != state.canFly()
			|| !next.players().equals(state.players());
		state = next;
		if (rebuild) {
			if (!next.admin()) page = PAGE_VOTING;
			clearAndInit();
		}
	}

	private void send(String action, String arg) {
		ClientPlayNetworking.send(new ActionPayload(action, arg));
	}

	private ButtonWidget button(String label, int x, int y, int w, Runnable r) {
		return addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> r.run()).dimensions(x, y, w, 20).build());
	}

	@Override
	protected void init() {
		int cx = width / 2;

		// tabs
		if (state.admin()) {
			button("Voting", cx - 165, 20, 106, () -> { page = PAGE_VOTING; clearAndInit(); });
			button("Admin Tools", cx - 53, 20, 106, () -> { page = PAGE_TOOLS; clearAndInit(); });
			button("Stick Enchants", cx + 59, 20, 106, () -> { page = PAGE_ENCHANTS; clearAndInit(); });
		}

		int y = 60;
		switch (page) {
			case PAGE_VOTING -> {
				List<String> players = state.players();
				if (players.isEmpty()) {
					// nothing to pick; election buttons are hidden
				} else {
					if (selected == null || !players.contains(selected)) selected = players.get(0);
					addDrawableChild(CyclingButtonWidget.<String>builder(Text::literal)
						.values(players).initially(selected)
						.build(cx - 100, y, 200, 20, Text.literal("Player"), (b, v) -> selected = v));
					button("Call election: make ADMIN", cx - 100, y + 26, 200, () -> send("elect", selected));
					button("Call election: REMOVE admin", cx - 100, y + 52, 200, () -> send("impeach", selected));
				}
				button("Vote YES", cx - 100, y + 90, 96, () -> send("vote", "yes"));
				button("Vote NO", cx + 4, y + 90, 96, () -> send("vote", "no"));
			}
			case PAGE_TOOLS -> {
				button("Survival", cx - 100, y, 98, () -> send("gamemode", "survival"));
				button("Creative", cx + 2, y, 98, () -> send("gamemode", "creative"));
				button("Adventure", cx - 100, y + 24, 98, () -> send("gamemode", "adventure"));
				button("Spectator", cx + 2, y + 24, 98, () -> send("gamemode", "spectator"));
				button("Flight in Survival: " + (state.canFly() ? "ON" : "OFF"), cx - 100, y + 54, 200,
					() -> send("fly", ""));
				String[] ids = {"perma", "1d", "7d", "1mo", "1yr"};
				for (int i = 0; i < ids.length; i++) {
					String id = ids[i];
					button(id, cx - 150 + i * 62, y + 100, 58, () -> send("banstick", id));
				}
			}
			case PAGE_ENCHANTS -> {
				int i = 0;
				for (String id : StickEnchants.ALL.keySet()) {
					button(StickEnchants.label(id) + " 255", cx - 154 + (i % 2) * 158, y + (i / 2) * 24, 150,
						() -> send("enchant", id));
					i++;
				}
				button("ALL enchantments 255", cx - 154 + (i % 2) * 158, y + (i / 2) * 24, 150,
					() -> send("enchant", "all"));
			}
			default -> { }
		}

		button("Close", cx - 50, height - 28, 100, this::close);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		int cx = width / 2;
		ctx.drawCenteredTextWithShadow(textRenderer, title, cx, 6, 0xFFFFFF);

		if (page == PAGE_VOTING) {
			Text status = state.election().isEmpty()
				? Text.literal("No election running.").formatted(Formatting.GRAY)
				: Text.literal(state.election()).formatted(Formatting.GOLD);
			ctx.drawCenteredTextWithShadow(textRenderer, status, cx, height - 48, 0xFFFFFF);
			if (state.players().isEmpty()) {
				ctx.drawCenteredTextWithShadow(textRenderer,
					Text.literal("No other players online to hold an election about."), cx, 64, 0xAAAAAA);
			}
		} else if (page == PAGE_ENCHANTS) {
			ctx.drawCenteredTextWithShadow(textRenderer,
				Text.literal("Hold a Ban Stick in your main hand, then click.").formatted(Formatting.GRAY),
				cx, height - 48, 0xFFFFFF);
		} else if (page == PAGE_TOOLS) {
			ctx.drawCenteredTextWithShadow(textRenderer,
				Text.literal("Get a Ban Stick:").formatted(Formatting.GRAY), cx, 60 + 88, 0xFFFFFF);
		}
	}

	@Override
	public void tick() {
		if (++ticks % 20 == 0) send("refresh", "");
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
