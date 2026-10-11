package dev.adminecraft;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** A fake player for testing Ban Sticks. Each spawned one gets the next number, saved per world. */
public class DebugPlayerEntity extends PathAwareEntity {
	private static int counter;
	private static Path counterFile;

	private int number;

	public DebugPlayerEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		setPersistent();
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return PathAwareEntity.createMobAttributes()
			.add(EntityAttributes.MAX_HEALTH, 20.0)
			.add(EntityAttributes.MOVEMENT_SPEED, 0.25);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new WanderAroundGoal(this, 0.6));
		goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
		goalSelector.add(3, new LookAroundGoal(this));
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}

	public int getNumber() {
		return number;
	}

	@Override
	public void tick() {
		super.tick();
		if (number == 0 && !getWorld().isClient) {
			number = nextNumber();
			setCustomName(Text.literal("Debug Player #" + number));
			setCustomNameVisible(true);
		}
	}

	@Override
	public void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("DebugNumber", number);
	}

	@Override
	public void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		number = nbt.getInt("DebugNumber");
	}

	// --- per-world counter ---

	public static void loadCounter(Path file) {
		counterFile = file;
		counter = 0;
		try {
			if (Files.exists(file)) counter = Integer.parseInt(Files.readString(file).trim());
		} catch (IOException | NumberFormatException e) {
			ADMINecraft.LOGGER.error("Could not read debug player counter", e);
		}
	}

	private static int nextNumber() {
		counter++;
		if (counterFile != null) {
			try {
				Files.writeString(counterFile, Integer.toString(counter));
			} catch (IOException e) {
				ADMINecraft.LOGGER.error("Could not save debug player counter", e);
			}
		}
		return counter;
	}
}
