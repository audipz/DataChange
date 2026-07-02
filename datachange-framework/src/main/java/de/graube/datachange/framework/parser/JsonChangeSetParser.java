package de.graube.datachange.framework.parser;

import tools.jackson.databind.ObjectMapper;
import de.graube.datachange.framework.model.ChangeSetDefinition;
import de.graube.datachange.framework.spi.ChangeSetParser;
import de.graube.datachange.framework.spi.ChangeSetSerializer;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses JSON files into ChangeSet definitions.
 */
@Component
public class JsonChangeSetParser implements ChangeSetParser, ChangeSetSerializer {

    private final ObjectMapper objectMapper;

    public JsonChangeSetParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChangeSetDefinition parse(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, ChangeSetDefinition.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Cannot parse changeset " + resource.getDescription(), ex);
        }
    }

    public String checksum(ChangeSetDefinition definition) {
        return serialize(definition).hashCode() + "";
    }

    @Override
    public String serialize(ChangeSetDefinition definition) {
        return objectMapper.writeValueAsString(definition);
    }
}

