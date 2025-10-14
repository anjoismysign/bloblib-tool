package io.github.anjoismysign.bloblibtool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class CodeGenerator {

    public static final class Field {
        public final String type;
        public final String name;
        public Field(String type, String name) {
            this.type = Objects.requireNonNull(type).trim();
            this.name = Objects.requireNonNull(name).trim();
        }
    }

    /**
     * Parse the input and generate the Java source string.
     *
     * Input format:
     *   ClassName: type name,boolean isMale,Passport passport
     */
    public static String generateSource(String input) {
        if (input == null) throw new InvalidDefinitionFormatException("Input is null");
        String trimmed = input.trim();
        if (trimmed.isEmpty()) throw new InvalidDefinitionFormatException("Input is empty");

        String[] parts = trimmed.split(":", 2); // must split into exactly 2 parts
        if (parts.length != 2) {
            throw new InvalidDefinitionFormatException("Definition must contain a single ':' separating class and fields. Example: Person:int age,boolean isMale");
        }

        String className = parts[0].trim();
        if (className.isEmpty()) {
            throw new InvalidDefinitionFormatException("Class name is empty.");
        }

        String fieldsPart = parts[1].trim();
        // split by comma for each field
        String[] rawFields = fieldsPart.isEmpty() ? new String[0] : fieldsPart.split(",");

        List<Field> fields = new ArrayList<>();
        for (String rawField : rawFields) {
            String rf = rawField.trim();
            if (rf.isEmpty()) continue;
            String[] kv = rf.split("\\s+", 2); // must be exactly 2 tokens: type and name
            if (kv.length != 2) {
                throw new InvalidFieldFormatException("Each field must be 'type name' (separated by space). Problematic field: '" + rf + "'");
            }
            fields.add(new Field(kv[0].trim(), kv[1].trim()));
        }

        return buildSource(className, fields);
    }

    private static String buildSource(String className, List<Field> fields) {
        StringBuilder sb = new StringBuilder();
        String nl = System.lineSeparator();

        // Record declaration: identifier first
        sb.append("public record ").append(className).append("(String identifier");
        for (Field f : fields) {
            sb.append(", ").append(f.type).append(" ").append(f.name);
        }
        sb.append(") implements DataAsset {").append(nl).append(nl);

        sb.append("    public static class Info implements IdentityGenerator<").append(className).append("> {").append(nl);
        // private fields
        for (Field f : fields) {
            sb.append("        private ").append(f.type).append(" ").append(f.name).append(";").append(nl);
        }
        if (!fields.isEmpty()) sb.append(nl);

        // generate method
        sb.append("        @Override").append(nl);
        sb.append("        public ").append(className).append(" generate(String identifier) {").append(nl);
        sb.append("            return new ").append(className).append("(identifier");
        for (Field f : fields) {
            sb.append(", ").append(f.name);
        }
        sb.append(");").append(nl);
        sb.append("        }").append(nl).append(nl);

        // getters & setters
        for (Field f : fields) {
            boolean isBoolean = "boolean".equals(f.type) || "Boolean".equals(f.type);
            String getterName;
            String setterName;

            if (isBoolean && isIsPrefix(f.name)) {
                // field like "isMale" -> getter is "isMale()", setter "setMale(...)"
                getterName = f.name; // uses the field name as getter: isMale()
                setterName = "set" + capitalize(stripIsPrefix(f.name));
            } else if (isBoolean) {
                getterName = "is" + capitalize(f.name);
                setterName = "set" + capitalize(f.name);
            } else {
                getterName = "get" + capitalize(f.name);
                setterName = "set" + capitalize(f.name);
            }

            // Getter
            sb.append("        public ").append(f.type).append(" ").append(getterName).append("() {").append(nl);
            sb.append("            return ").append(f.name).append(";").append(nl);
            sb.append("        }").append(nl).append(nl);

            // Setter
            sb.append("        public void ").append(setterName).append("(").append(f.type).append(" ").append(f.name).append(") {").append(nl);
            sb.append("            this.").append(f.name).append(" = ").append(f.name).append(";").append(nl);
            sb.append("        }").append(nl).append(nl);
        }

        sb.append("    }").append(nl).append(nl);
        sb.append("}").append(nl);

        return sb.toString();
    }

    private static boolean isIsPrefix(String name) {
        if (name == null) return false;
        if (!name.startsWith("is")) return false;
        if (name.length() <= 2) return false; // "is" only -> not acceptable
        // treat as boolean-is-prefixed only if next char is uppercase (isMale) to avoid words like "island"
        char c = name.charAt(2);
        return Character.isUpperCase(c);
    }

    private static String stripIsPrefix(String name) {
        if (!isIsPrefix(name)) return name;
        return name.substring(2);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
