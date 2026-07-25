package io.github.omegat.ai;

/** Small JSON helper to keep the plugin JAR dependency-free. */
final class Json {
    private Json() {
    }

    static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 16).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\b':
                    out.append("\\b");
                    break;
                case '\f':
                    out.append("\\f");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                    break;
            }
        }
        return out.append('"').toString();
    }

    static String firstStringValue(String json, String key) {
        String needle = "\"" + key + "\"";
        int position = json.indexOf(needle);
        while (position >= 0) {
            int colon = skipWhitespace(json, position + needle.length());
            if (colon < json.length() && json.charAt(colon) == ':') {
                int quote = skipWhitespace(json, colon + 1);
                if (quote < json.length() && json.charAt(quote) == '"') {
                    return readString(json, quote);
                }
            }
            position = json.indexOf(needle, position + needle.length());
        }
        return null;
    }

    private static int skipWhitespace(String value, int from) {
        while (from < value.length() && Character.isWhitespace(value.charAt(from))) from++;
        return from;
    }

    private static String readString(String json, int openingQuote) {
        StringBuilder value = new StringBuilder();
        for (int i = openingQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"') return value.toString();
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (++i >= json.length()) break;
            char escaped = json.charAt(i);
            switch (escaped) {
                case '"':
                case '\\':
                case '/':
                    value.append(escaped);
                    break;
                case 'b':
                    value.append('\b');
                    break;
                case 'f':
                    value.append('\f');
                    break;
                case 'n':
                    value.append('\n');
                    break;
                case 'r':
                    value.append('\r');
                    break;
                case 't':
                    value.append('\t');
                    break;
                case 'u':
                    if (i + 4 >= json.length()) throw new IllegalArgumentException("Invalid JSON escape");
                    value.append((char) Integer.parseInt(json.substring(i + 1, i + 5), 16));
                    i += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid JSON escape");
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }
}
