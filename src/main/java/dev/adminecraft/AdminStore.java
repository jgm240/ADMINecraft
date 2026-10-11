package dev.adminecraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Persists the set of elected admins (uuid -> last known name) in the world folder. */
public final class AdminStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final Map<String, String> admins = new LinkedHashMap<>();
	private Path file;

	public void load(Path file) {
		this.file = file;
		admins.clear();
		if (!Files.exists(file)) return;
		try (Reader r = Files.newBufferedReader(file)) {
			Map<String, String> loaded = GSON.fromJson(r, new TypeToken<Map<String, String>>() {}.getType());
			if (loaded != null) admins.putAll(loaded);
		} catch (IOException | RuntimeException e) {
			ADMINecraft.LOGGER.error("Could not read admin list", e);
		}
	}

	public void save() {
		if (file == null) return;
		try (Writer w = Files.newBufferedWriter(file)) {
			GSON.toJson(admins, w);
		} catch (IOException e) {
			ADMINecraft.LOGGER.error("Could not save admin list", e);
		}
	}

	public boolean isElected(UUID id) {
		return admins.containsKey(id.toString());
	}

	/** Elected admins and server operators both count as admins. */
	public boolean isAdmin(MinecraftServer server, GameProfile profile) {
		return isElected(profile.getId()) || server.getPlayerManager().isOperator(profile);
	}

	public void add(GameProfile profile) {
		admins.put(profile.getId().toString(), profile.getName());
		save();
	}

	public boolean remove(UUID id) {
		boolean removed = admins.remove(id.toString()) != null;
		if (removed) save();
		return removed;
	}

	public Map<String, String> all() {
		return Map.copyOf(admins);
	}
}
