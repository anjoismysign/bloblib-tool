package io.github.anjoismysign.bloblibtool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class CodeGenerator {

    public record Field(String type, String name, String getter, String setter) {
        public static Field of(String type, String name) {
            boolean isBoolean = "boolean".equals(type) || "Boolean".equals(type);
            String getter;
            String setter;

            if (isBoolean && isIsPrefix(name)) {
                // field like "isMale" -> getter is "isMale()", setter "setMale(...)"
                getter = name; // uses the field name as getter: isMale()
                setter = "set" + capitalize(stripIsPrefix(name));
            } else if (isBoolean) {
                getter = "is" + capitalize(name);
                setter = "set" + capitalize(name);
            } else {
                getter = "get" + capitalize(name);
                setter = "set" + capitalize(name);
            }
            return new Field(type, name, getter, setter);
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
            fields.add(Field.of(kv[0].trim(), kv[1].trim()));
        }

        return buildSource(className, fields);
    }

    private static String buildSource(String className, List<Field> fields) {
        StringBuilder builder = new StringBuilder();
        String newLine = System.lineSeparator();
        builder.append("""
                import io.github.anjoismysign.holoworld.asset.IdentityDataAsset;
                import io.github.anjoismysign.holoworld.asset.IdentityGenerator;
                import org.jetbrains.annotations.NotNull;
                """).append(newLine);

        // Record declaration: identifier first
        builder.append("public record ").append(className).append("(String identifier");
        for (Field f : fields) {
            builder.append(", ").append(f.type).append(" ").append(f.name);
        }
        builder.append(") implements IdentityDataAsset<"+className+"> {").append(newLine).append(newLine);

        builder.append("    @Override").append(newLine);
        builder.append("    @NotNull").append(newLine);
        builder.append("    public IdentityGenerator<").append(className).append("> generator(){").append(newLine);
        builder.append("        Info info = new Info();").append(newLine);
        for (Field field : fields) {
            builder.append("        info.").append(field.setter).append("(").append(field.name).append(");").append(newLine);
        }
        builder.append("        return info;").append(newLine);
        builder.append("    }").append(newLine).append(newLine);


        builder.append("    public static class Info implements IdentityGenerator<").append(className).append("> {").append(newLine);
        // private fields
        for (Field field : fields) {
            builder.append("        private ").append(field.type).append(" ").append(field.name).append(";").append(newLine);
        }
        if (!fields.isEmpty()) builder.append(newLine);

        // generate method
        builder.append("        @Override").append(newLine);
        builder.append("        public ").append(className).append(" generate(String identifier){").append(newLine);
        builder.append("            return new ").append(className).append("(identifier");
        for (Field field : fields) {
            builder.append(", ").append(field.name);
        }
        builder.append(");").append(newLine);
        builder.append("        }").append(newLine).append(newLine);

        // getters & setters
        for (Field field : fields) {
            String getter = field.getter();
            String setter = field.setter();

            // Getter
            builder.append("        public ").append(field.type).append(" ").append(getter).append("() {").append(newLine);
            builder.append("            return ").append(field.name).append(";").append(newLine);
            builder.append("        }").append(newLine).append(newLine);

            // Setter
            builder.append("        public void ").append(setter).append("(").append(field.type).append(" ").append(field.name).append(") {").append(newLine);
            builder.append("            this.").append(field.name).append(" = ").append(field.name).append(";").append(newLine);
            builder.append("        }").append(newLine).append(newLine);
        }

        builder.append("    }").append(newLine).append(newLine);
        builder.append("}").append(newLine);

        return builder.toString();
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
