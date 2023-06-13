package com.github.tezvn.starpvp.core.utils.time;

import java.util.Arrays;
import java.util.Optional;

/**
 * Classes that represents time related untis
 */
public enum TimeUnits {
	/**
	 * Represents day value
	 */
	DAY("d"),
	/**
	 * Represents hour value
	 */
	HOUR("h"),
	/**
	 * Represents minute value
	 */
	MINUTE("m"),
	/**
	 * Represents month value
	 */
	MONTH("M"),
	/**
	 * Represents second value
	 */
	SECOND("s"),
	/**
	 * Represents week value
	 */
	WEEK("w"),
	/**
	 * Represents year value
	 */
	YEAR("y");

	private String key;

	TimeUnits(String key) {
		this.key = key;
	}

	/**
	 * Get time unit key
	 * @return Time unit key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Parses the string argument as a signed time unit key
	 * @param str String to parse
	 * @return The time unit key
	 */
	public static Optional<TimeUnits> parse(String str) {
		return Arrays.asList(TimeUnits.values()).stream()
				.filter(u -> u.getKey().equals(str))
				.findAny();
	}


}
