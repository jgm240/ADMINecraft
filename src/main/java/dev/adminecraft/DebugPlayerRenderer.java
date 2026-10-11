package dev.adminecraft;

import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.util.Identifier;

/** Renders the Debug Player with the vanilla player model and a hay-bale skin. */
public class DebugPlayerRenderer extends BipedEntityRenderer<DebugPlayerEntity, PlayerEntityRenderState, PlayerEntityModel> {
	private static final Identifier TEXTURE = Identifier.of(ADMINecraft.MOD_ID, "textures/entity/debug_player.png");

	public DebugPlayerRenderer(EntityRendererFactory.Context ctx) {
		super(ctx, new PlayerEntityModel(ctx.getPart(EntityModelLayers.PLAYER), false), 0.5f);
	}

	@Override
	public PlayerEntityRenderState createRenderState() {
		return new PlayerEntityRenderState();
	}

	@Override
	public Identifier getTexture(PlayerEntityRenderState state) {
		return TEXTURE;
	}
}
