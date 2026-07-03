package io.github.audipz.datachange.framework.loader;

import io.github.audipz.datachange.framework.config.DataChangeProperties;
import io.github.audipz.datachange.framework.model.ChangeSetDefinition;
import io.github.audipz.datachange.framework.model.TransactionMode;
import io.github.audipz.datachange.framework.parser.JsonChangeSetParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResourceChangeSetSourceTest {

    @Test
    void shouldFilterByIncludeExcludeIdsAndLabels() {
        DataChangeProperties properties = new DataChangeProperties();
        properties.setLocations(List.of("classpath*:datachange/source-filter/*.json"));
        properties.setIncludeIds(List.of("cs-a", "cs-b"));
        properties.setExcludeIds(List.of("cs-b"));
        properties.setIncludeLabels(List.of("core"));

        JsonChangeSetParser parser = mock(JsonChangeSetParser.class);
        when(parser.parse(argThat(resourceName("001-a.json")))).thenReturn(changeSet("cs-a", List.of("core")));
        when(parser.parse(argThat(resourceName("002-b.json")))).thenReturn(changeSet("cs-b", List.of("core")));
        when(parser.parse(argThat(resourceName("003-c.json")))).thenReturn(changeSet("cs-c", List.of("tenant")));

        ResourceChangeSetSource source = new ResourceChangeSetSource(properties, parser);

        List<ChangeSetDefinition> result = source.load();

        assertEquals(1, result.size());
        assertEquals("cs-a", result.getFirst().id());
    }

    @Test
    void shouldReturnAllWhenNoFiltersAreConfigured() {
        DataChangeProperties properties = new DataChangeProperties();
        properties.setLocations(List.of("classpath*:datachange/source-filter/*.json"));

        JsonChangeSetParser parser = mock(JsonChangeSetParser.class);
        when(parser.parse(argThat(resourceName("001-a.json")))).thenReturn(changeSet("cs-a", List.of("core")));
        when(parser.parse(argThat(resourceName("002-b.json")))).thenReturn(changeSet("cs-b", List.of("core")));
        when(parser.parse(argThat(resourceName("003-c.json")))).thenReturn(changeSet("cs-c", List.of("tenant")));

        ResourceChangeSetSource source = new ResourceChangeSetSource(properties, parser);

        List<ChangeSetDefinition> result = source.load();

        assertEquals(3, result.size());
        assertEquals(List.of("cs-a", "cs-b", "cs-c"), result.stream().map(ChangeSetDefinition::id).toList());
    }

    private static ChangeSetDefinition changeSet(String id, List<String> labels) {
        return new ChangeSetDefinition(
                "1.0",
                id,
                "tester",
                "desc",
                labels,
                List.of(),
                TransactionMode.PER_CHANGE,
                null,
                List.of()
        );
    }

    private static org.mockito.ArgumentMatcher<Resource> resourceName(String expectedFilename) {
        return resource -> resource != null && expectedFilename.equals(resource.getFilename());
    }
}

