package dev.adminecraft;

import java.util.List;
import java.util.concurrent.TimeUnit;

public enum BanDuration {
	PERMA("perma", "Permanent", -1),
	DAY("1d", "1 Day", TimeUnit.DAYS.toMillis(1)),
	WEEK("7d", "7 Days", TimeUnit.DAYS.toMillis(7)),
	MONTH("1mo", "1 Month", TimeUnit.DAYS.toMillis(30)),
	YEAR("1yr", "1 Year", TimeUnit.DAYS.toMillis(365));

	public final String id;
	public final String label;
	public final long millis;

	BanDuration(String id, String label, long millis) {
		this.id = id;
		this.label = label;
		this.millis = millis;
	}

	public boolean isPermanent() {
		return millis < 0;
	}

	public static BanDuration byId(String id) {
		for (BanDuration d : values()) {
			if (d.id.equalsIgnoreCase(id)) return d;
		}
		return null;
	}

	public static List<String> ids() {
		return java.util.Arrays.stream(values()).map(d -> d.id).toList();
	}
}
