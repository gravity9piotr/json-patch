package com.github.fge.jsonpatch;

public class JsonPathParser {

    private static final String ARRAY_ELEMENT_REGEX = "(?<=\\.)(\\d+)";

    public static String tmfStringToJsonPath(String path) throws JsonPatchException {
        if (path.startsWith("$")) {
            return path;
        } else if (path.contains("?")) {
            throw new JsonPatchException("Invalid path, `?` are not allowed in JsonPointer expressions.");
        } else if (path.contains("//")) {
            throw new JsonPatchException("Invalid path, `//` is not allowed in JsonPointer expressions.");
        }

        return "$" + path.replace('/', '.').replaceAll(ARRAY_ELEMENT_REGEX, "[$1]");
    }
}
