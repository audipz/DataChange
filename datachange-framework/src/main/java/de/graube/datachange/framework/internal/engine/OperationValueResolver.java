package de.graube.datachange.framework.internal.engine;

import de.graube.datachange.framework.api.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves ${ref('...')} placeholders in operation values.
 */
public final class OperationValueResolver {

    private static final Pattern REF_PATTERN = Pattern.compile("\\$\\{ref\\('([^']+)'\\)}");

    private OperationValueResolver() {
    }

    public static Object resolveValue(Object raw, ExecutionContext context) {
        if (raw instanceof String str) {
            Matcher matcher = REF_PATTERN.matcher(str);
            if (matcher.matches()) {
                return context.get(matcher.group(1));
            }
        }
        if (raw instanceof List<?> list) {
            List<Object> resolved = new ArrayList<>();
            for (Object it : list) {
                resolved.add(resolveValue(it, context));
            }
            return resolved;
        }
        return raw;
    }

    public static void storeReference(String saveAs, Object entity, ExecutionContext context) {
        if (saveAs != null && !saveAs.isBlank()) {
            context.put(saveAs, entity);
        }
    }
}

