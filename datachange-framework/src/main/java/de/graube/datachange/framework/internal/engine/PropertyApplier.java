package de.graube.datachange.framework.internal.engine;

import de.graube.datachange.framework.api.ExecutionContext;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Map;

/**
 * Applies values onto a JPA entity using Spring's property access.
 */
public final class PropertyApplier {

    private PropertyApplier() {
    }

    public static long applyValues(Object entity, Map<String, Object> values, ExecutionContext context, ObjectMapper objectMapper) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        BeanWrapper wrapper = new BeanWrapperImpl(entity);
        long touched = 0;

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object newValue = OperationValueResolver.resolveValue(entry.getValue(), context);
            Object oldValue = wrapper.getPropertyValue(entry.getKey());
            Object converted = objectMapper.convertValue(newValue, wrapper.getPropertyType(entry.getKey()));
            if ((oldValue == null && converted != null) || (oldValue != null && !oldValue.equals(converted))) {
                wrapper.setPropertyValue(entry.getKey(), converted);
                touched++;
            }
        }
        return touched;
    }
}

