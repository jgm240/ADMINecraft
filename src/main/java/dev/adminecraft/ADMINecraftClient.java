package dev.adminecraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class ADMINecraftClient implements ClientModInitializer {
	private static KeyBinding openKey;

	@Override
	public void onInitializeClient() {
		openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			"key.adminecraft.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_I, "category.adminecraft"));

		EntityRendererRegistry.register(ModEntities.DEBUG_PLAYER, DebugPlayerRenderer::new);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.wasPressed()) {
				if (client.currentScreen == null && client.player != null) {
					ClientPlayNetworking.send(new ActionPayload("open", ""));
				}
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(StatePayload.ID, (payload, context) -> {
			MinecraftClient client = context.client();
			if (client.currentScreen instanceof AdminScreen screen) {
				screen.update(payload);
			} else if (payload.openScreen() && client.currentScreen == null) {
				client.setScreen(new AdminScreen(payload));
			}
		});
	}
}
