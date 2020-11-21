package com.christian34.easyprefix.utils;

/**
 * EasyPrefix 2020.
 *
 * @author Christian34
 */
public enum ChatFormatting {
    BOLD("l", Message.FORMATTING_BOLD),
    ITALIC("o", Message.FORMATTING_ITALIC),
    RAINBOW("r", Message.FORMATTING_RAINBOW),
    STRIKETHROUGH("m", Message.FORMATTING_STRIKETHROUGH),
    UNDEFINED("", null),
    UNDERLINE("n", Message.FORMATTING_UNDERLINE);

    private final String code;
    private final Message name;

    ChatFormatting(String code, Message name) {
        this.code = code;
        this.name = name;
    }

    public static ChatFormatting getByCode(String code) {
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.code.equals(code)) return formatting;
        }
        return null;
    }

    public static ChatFormatting[] getValues() {
        ChatFormatting[] formattings = new ChatFormatting[values().length - 1];
        int i = 0;
        for (ChatFormatting formatting : values()) {
            if (formatting == UNDEFINED) continue;
            formattings[i] = formatting;
            i++;
        }
        return formattings;
    }

    @Override
    public String toString() {
        if (code != null) {
            if (code.equals("r")) {
                return getCode() + RainbowEffect.addRainbowEffect(name.getText());
            }
            return getCode() + name.getText();
        }
        return "";
    }

    public String getCode() {
        return code == null ? "" : "§" + code;
    }

}
