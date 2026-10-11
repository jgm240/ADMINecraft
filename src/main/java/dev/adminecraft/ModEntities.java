package dev.adminecraft;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
	private static final Identifier ID = Identifier.of(ADMINecraft.MOD_ID, "debug_player");

	public static final EntityType<DebugPlayerEntity> DEBUG_PLAYER = Registry.register(
		Registries.ENTITY_TYPE, ID,
		EntityType.Builder.create(DebugPlayerEntity::new, SpawnGroup.MISC)
			.dimensions(0.6f, 1.8f)
			.maxTrackingRange(10)
			.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, ID)));

	public static final Item DEBUG_PLAYER_SPAWN_EGG = Registry.register(
		Registries.ITEM, Identifier.of(ADMINecraft.MOD_ID, "debug_player_spawn_egg"),
		new SpawnEggItem(DEBUG_PLAYER, new Item.Settings().registryKey(
			RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ADMINecraft.MOD_ID, "debug_player_spawn_egg")))));

	private ModEntities() {}

	public static void register() {
		FabricDefaultAttributeRegistry.register(DEBUG_PLAYER, DebugPlayerEntity.createAttributes());
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.SPAWN_EGGS).register(e -> e.add(DEBUG_PLAYER_SPAWN_EGG));
	}
}
