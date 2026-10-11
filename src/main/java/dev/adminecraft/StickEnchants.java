package dev.adminecraft;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.RegistryKey;

import java.util.LinkedHashMap;
import java.util.Map;

/** Enchantments admins may put on a Ban Stick, always at level 255. */
public final class StickEnchants {
	public static final int LEVEL = 255;
	public static final Map<String, RegistryKey<Enchantment>> ALL = new LinkedHashMap<>();

	static {
		ALL.put("sharpness", Enchantments.SHARPNESS);
		ALL.put("smite", Enchantments.SMITE);
		ALL.put("bane_of_arthropods", Enchantments.BANE_OF_ARTHROPODS);
		ALL.put("knockback", Enchantments.KNOCKBACK);
		ALL.put("fire_aspect", Enchantments.FIRE_ASPECT);
		ALL.put("looting", Enchantments.LOOTING);
		ALL.put("sweeping_edge", Enchantments.SWEEPING_EDGE);
		ALL.put("unbreaking", Enchantments.UNBREAKING);
		ALL.put("mending", Enchantments.MENDING);
	}

	private StickEnchants() {}

	public static String label(String id) {
		StringBuilder sb = new StringBuilder();
		for (String part : id.split("_")) {
			if (sb.length() > 0) sb.append(' ');
			sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return sb.toString();
	}
}
