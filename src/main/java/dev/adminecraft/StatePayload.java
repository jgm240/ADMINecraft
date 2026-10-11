package dev.adminecraft;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.List;

/** Server -> client: what the GUI needs to render. */
public record StatePayload(boolean openScreen, boolean admin, boolean canFly, String election, List<String> players)
	implements CustomPayload {
	public static final Id<StatePayload> ID = new Id<>(Identifier.of(ADMINecraft.MOD_ID, "state"));
	public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.tuple(
		PacketCodecs.BOOLEAN, StatePayload::openScreen,
		PacketCodecs.BOOLEAN, StatePayload::admin,
		PacketCodecs.BOOLEAN, StatePayload::canFly,
		PacketCodecs.STRING, StatePayload::election,
		PacketCodecs.STRING.collect(PacketCodecs.toList()), StatePayload::players,
		StatePayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
