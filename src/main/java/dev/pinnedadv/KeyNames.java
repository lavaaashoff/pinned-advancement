package dev.pinnedadv;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;

/** English key names by physical key (SDL scancode), independent of the keyboard layout. */
public final class KeyNames {
	private KeyNames() {}

	public static Component name(int sc) {
		String s = plain(sc);
		return s != null ? Component.literal(s) : InputConstants.Type.KEYBOARD.getOrCreate(sc).getDisplayName();
	}

	private static String plain(int sc) {
		if (sc >= 4 && sc <= 29) return String.valueOf((char) ('A' + sc - 4));
		if (sc >= 30 && sc <= 38) return String.valueOf((char) ('1' + sc - 30));
		if (sc >= 58 && sc <= 69) return "F" + (sc - 57);
		if (sc >= 104 && sc <= 115) return "F" + (sc - 91);
		if (sc >= 89 && sc <= 97) return "Num " + (sc - 88);
		return switch (sc) {
			case 39 -> "0";
			case 40 -> "Enter";
			case 42 -> "Backspace";
			case 43 -> "Tab";
			case 44 -> "Space";
			case 45 -> "-";
			case 46 -> "=";
			case 47 -> "[";
			case 48 -> "]";
			case 49 -> "\\";
			case 51 -> ";";
			case 52 -> "'";
			case 53 -> "`";
			case 54 -> ",";
			case 55 -> ".";
			case 56 -> "/";
			case 57 -> "Caps Lock";
			case 70 -> "Print Screen";
			case 71 -> "Scroll Lock";
			case 72 -> "Pause";
			case 73 -> "Insert";
			case 74 -> "Home";
			case 75 -> "Page Up";
			case 76 -> "Delete";
			case 77 -> "End";
			case 78 -> "Page Down";
			case 79 -> "Right";
			case 80 -> "Left";
			case 81 -> "Down";
			case 82 -> "Up";
			case 83 -> "Num Lock";
			case 84 -> "Num /";
			case 85 -> "Num *";
			case 86 -> "Num -";
			case 87 -> "Num +";
			case 88 -> "Num Enter";
			case 98 -> "Num 0";
			case 99 -> "Num .";
			default -> null;
		};
	}
}
