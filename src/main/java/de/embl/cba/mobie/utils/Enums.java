package de.embl.cba.mobie.utils;

public class Enums
{
	public static <T extends Enum<?>> T valueOf( Class<T> enumeration,
												 String search) {
		for (T each : enumeration.getEnumConstants()) {
			if (each.name().compareToIgnoreCase(search) == 0) {
				return each;
			}
		}
		return null;
	}
}
