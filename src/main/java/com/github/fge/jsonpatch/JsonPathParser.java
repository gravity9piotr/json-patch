package com.github.fge.jsonpatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonPathParser {

    private static final String ARRAY_ELEMENT_REGEX = "\\.(\\d+)";

    public static String tmfStringToJsonPath(String path) throws JsonPatchException {
        if ("/".equals(path) || path.isEmpty()) {
            return "$";
        }
        if (path.startsWith("$")) {
            return path;
        }
        if (!path.startsWith("/")) {
            return "$." + path;
        }
        final String[] pointerAndQuery = path.split("\\?", 2);
        final String jsonPath = "$" + pointerAndQuery[0].replace('/', '.').replaceAll(ARRAY_ELEMENT_REGEX, ".[$1]");
        final String jsonPathWithQuery = addQueryIfApplicable(jsonPath, pointerAndQuery);
        return jsonPathWithQuery;
    }

    private static String addQueryIfApplicable(final String jsonPath, final String[] pointerAndQuery) {
        if (pointerAndQuery.length == 2) {
            String[] preparedFilters = pointerAndQuery[1]
                    .replaceAll("]", "] empty false") // add empty false to nested array expressions
                    .replaceAll("(\\w)=(\\w)", "$1==$2") // replace single equals with double
                    .replaceAll("==([\\w .]+)", "=='$1'") // surround strings with single quotes
                    .split("(?<!&)&(?!&)", -1); // split on single & characters, | are not supported
            Map<String, List<String>> filtersByPath = new HashMap<>();
            for (String preparedFilter : preparedFilters) {
                String path = preparedFilter.split("\\.", 2)[0];
                List<String> filters = filtersByPath.get(path);
                if (filters == null) {
                    filters = new ArrayList<>();
                }
                filters.add(filterHandlingPrimitives(preparedFilter).replace(path, "@"));
                filtersByPath.put(path, filters);
            }
            String expression = jsonPath;
            for (Map.Entry<String, List<String>> entry : filtersByPath.entrySet()) {
                String path = entry.getKey();
                String filters = mergeFilters(entry.getValue());
                // negative lookahead used to not add filters inside previous filters
                expression = expression.replaceAll(path + "(?!.*\\])", path + "[?(" + filters + ")]");
            }

            return expression;
        } else {
            return jsonPath;
        }
    }

    private static String filterHandlingPrimitives(String filter) {
        return filter
                .replaceAll("([\\w.]+)=='(true|false)'", "($1==$2 || $1=='$2')") // prepare a statement for boolean and boolean as string
                .replaceAll("([\\w.]+)=='(\\d+)'", "($1==$2 || $1=='$2')") // prepare a statement for an integer and integer as string
                .replaceAll("([\\w.]+)=='(\\d+\\.\\d+)'", "($1==$2 || $1=='$2')"); // prepare a statement for float and float as string
    }

    private static String mergeFilters(List<String> filters) {
        return Arrays.toString(filters.toArray())
                .replaceAll(", ", " && ").substring(1).replaceFirst("]$", "");
    }
}
