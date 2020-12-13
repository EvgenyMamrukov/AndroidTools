package utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StringUtils {

    public static String capitalize(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }

        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    public static String quote(@Nullable String string) {
        if (string == null || string.isEmpty()) {
            return "";
        }

        return '"' + string + '"';
    }
    public static String convertCamelToSnake(
            @NotNull String camelCaseString, @Nullable String prefix, @Nullable String postfix, boolean toUpperCase
    ) {
        final String[] stringWords = camelCaseString.split("(?=\\p{Upper})");
        final StringBuilder stringBuilder = new StringBuilder((prefix != null && !prefix.isEmpty()) ? prefix : "");

        for (String word : stringWords) {
            if (!word.equalsIgnoreCase(prefix)) {
                if (!stringBuilder.toString().isEmpty()) {
                    stringBuilder.append("_");
                }
                stringBuilder.append(word.toLowerCase());
            }
        }

        if (postfix != null && !postfix.isEmpty()) {
            stringBuilder.append("_").append(postfix);
        }

        final String convertedString;
        if (toUpperCase) {
            convertedString = stringBuilder.toString().toUpperCase();
        } else {
            convertedString = stringBuilder.toString();
        }

        return convertedString;
    }
}
