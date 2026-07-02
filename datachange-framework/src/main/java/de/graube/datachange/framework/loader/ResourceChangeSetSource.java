package de.graube.datachange.framework.loader;

import de.graube.datachange.framework.api.ChangeSetSource;
import de.graube.datachange.framework.config.DataChangeProperties;
import de.graube.datachange.framework.model.ChangeSetDefinition;
import de.graube.datachange.framework.spi.ChangeSetParser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads ChangeSets from configured resource locations.
 */
@Component
public class ResourceChangeSetSource implements ChangeSetSource {

    private final DataChangeProperties properties;
    private final ChangeSetParser parser;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public ResourceChangeSetSource(DataChangeProperties properties, ChangeSetParser parser) {
        this.properties = properties;
        this.parser = parser;
    }

    @Override
    public List<ChangeSetDefinition> load() {
        List<Resource> resources = new ArrayList<>();
        for (String location : properties.getLocations()) {
            try {
                for (Resource resource : resolver.getResources(location)) {
                    if (resource.exists()) {
                        resources.add(resource);
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot read changeset location " + location, ex);
            }
        }

        resources.sort(Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String::compareTo)));

        List<ChangeSetDefinition> result = new ArrayList<>();
        for (Resource resource : resources) {
            ChangeSetDefinition changeSet = parser.parse(resource);
            if (matchesFilters(changeSet)) {
                result.add(changeSet);
            }
        }
        return result;
    }

    private boolean matchesFilters(ChangeSetDefinition changeSet) {
        Set<String> includeIds = new HashSet<>(properties.getIncludeIds());
        if (!includeIds.isEmpty() && !includeIds.contains(changeSet.id())) {
            return false;
        }

        Set<String> excludeIds = new HashSet<>(properties.getExcludeIds());
        if (excludeIds.contains(changeSet.id())) {
            return false;
        }

        Set<String> includeLabels = new HashSet<>(properties.getIncludeLabels());
        if (includeLabels.isEmpty()) {
            return true;
        }

        List<String> labels = changeSet.labels() == null ? List.of() : changeSet.labels();
        for (String label : labels) {
            if (includeLabels.contains(label)) {
                return true;
            }
        }
        return false;
    }
}

