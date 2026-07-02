package de.graube.datachange.framework.spi;

import de.graube.datachange.framework.model.ChangeSetDefinition;
import org.springframework.core.io.Resource;

/**
 * Parses a resource into a {@link ChangeSetDefinition} and can serialize it for checksum calculation.
 */
public interface ChangeSetParser {

    ChangeSetDefinition parse(Resource resource);

    String checksum(ChangeSetDefinition definition);
}

