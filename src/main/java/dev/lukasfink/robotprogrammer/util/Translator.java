package dev.lukasfink.robotprogrammer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Translator {

    private static final HashMap<String, String> translations = new HashMap<>();

    static {
        translations.put("instruction.init", "Start");
        translations.put("instruction.forward", "Vorwärts");
        translations.put("instruction.backwards", "Rückwärts");
        translations.put("instruction.turn_left", "Links");
        translations.put("instruction.turn_right", "Rechts");
        translations.put("instruction.terminate", "Ende");
        translations.put("instruction.melody", "Melodie");
        translations.put("instruction.blink", "Blinken");
    }

    public static String translate(String input) {
        for (Map.Entry<String, String> translation: translations.entrySet()) {
            input = input.replaceAll(Pattern.quote("${" + translation.getKey() + "}"), translation.getValue());
        }

        return input;
    }

}
