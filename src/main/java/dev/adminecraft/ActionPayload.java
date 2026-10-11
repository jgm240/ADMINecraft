package dev.adminecraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** Client -> server: a GUI button press. The server re-validates everything. */
public record ActionPayload(String action, String arg) implements CustomPayload {
	public static final Id<ActionPayload> ID = new Id<>(Identifier.of(ADMINecraft.MOD_ID, "action"));
	public static final PacketCodec<RegistryByteBuf, ActionPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING, ActionPayload::action,
		PacketCodecs.STRING, ActionPayload::arg,
		ActionPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
