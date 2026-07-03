package io.github.audipz.datachange.framework.rest;

import io.github.audipz.datachange.framework.api.ChangeSetSource;
import io.github.audipz.datachange.framework.api.DataChangeExecutionResult;
import io.github.audipz.datachange.framework.api.DataChangeExecutor;
import io.github.audipz.datachange.framework.model.ChangeSetDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for manual ChangeSet execution.
 */
@RestController
@RequestMapping("/datachange")
public class DataChangeController {

    private final ChangeSetSource source;
    private final DataChangeExecutor executor;

    public DataChangeController(ChangeSetSource source, DataChangeExecutor executor) {
        this.source = source;
        this.executor = executor;
    }

    @GetMapping("/changesets")
    public List<DataChangeMetaDto> listChangesets() {
        return source.load().stream()
                .map(cs -> new DataChangeMetaDto(cs.id(), cs.author(), cs.description()))
                .collect(Collectors.toList());
    }

    @PostMapping("/execute")
    public DataChangeExecutionResult executeById(@RequestParam String id) {
        ChangeSetDefinition changeSet = source.load().stream()
                .filter(cs -> cs.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("ChangeSet not found: " + id));
        return executor.execute(changeSet);
    }

    public record DataChangeMetaDto(String id, String author, String description) {
    }
}

