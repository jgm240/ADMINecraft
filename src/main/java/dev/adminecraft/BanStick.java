package dev.adminecraft;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class BanStick {
	private static final String KEY = "adminecraft_banstick";

	private BanStick() {}

	public static ItemStack create(BanDuration d) {
		ItemStack stack = new ItemStack(Items.STICK);
		NbtCompound tag = new NbtCompound();
		tag.putString(KEY, d.id);
		stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
		stack.set(DataComponentTypes.CUSTOM_NAME,
			Text.literal("Ban Stick [" + d.label + "]").formatted(Formatting.RED, Formatting.BOLD)
				.styled(s -> s.withItalic(false)));
		stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(
			Text.literal("Punch a player to ban them.").formatted(Formatting.GRAY),
			Text.literal("Duration: " + d.label).formatted(Formatting.DARK_RED),
			Text.literal("Admins only.").formatted(Formatting.DARK_GRAY))));
		stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	/** Returns the stick's ban duration, or null if the stack is not a Ban Stick. */
	public static BanDuration read(ItemStack stack) {
		if (stack.isEmpty()) return null;
		NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (data == null) return null;
		NbtCompound tag = data.copyNbt();
		if (!tag.contains(KEY)) return null;
		return BanDuration.byId(tag.getString(KEY));
	}
}
