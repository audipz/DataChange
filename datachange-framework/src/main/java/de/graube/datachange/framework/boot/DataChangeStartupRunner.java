package de.graube.datachange.framework.boot;

import de.graube.datachange.framework.api.ChangeSetSource;
import de.graube.datachange.framework.api.DataChangeExecutor;
import de.graube.datachange.framework.config.DataChangeFailureStrategy;
import de.graube.datachange.framework.config.DataChangeMode;
import de.graube.datachange.framework.config.DataChangeProperties;
import de.graube.datachange.framework.model.ChangeSetDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Executes ChangeSets on startup when configured.
 */
public class DataChangeStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataChangeStartupRunner.class);

    private final ChangeSetSource source;
    private final DataChangeExecutor executor;
    private final DataChangeProperties properties;

    public DataChangeStartupRunner(ChangeSetSource source, DataChangeExecutor executor, DataChangeProperties properties) {
        this.source = source;
        this.executor = executor;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled() || properties.getMode() != DataChangeMode.STARTUP) {
            return;
        }

        validateConfiguration();

        List<ChangeSetDefinition> changeSets = source.load();
        for (ChangeSetDefinition changeSet : changeSets) {
            if (properties.isDryRun()) {
                log.info("[dry-run] ChangeSet {} would be executed", changeSet.id());
                continue;
            }

            try {
                var result = executor.execute(changeSet);
                log.info("ChangeSet {} -> status={}, inserts={}, updates={}, deletes={}",
                        result.changeSetId(), result.status(), result.inserts(), result.updates(), result.deletes());
            } catch (RuntimeException ex) {
                if (properties.getFailureStrategy() == DataChangeFailureStrategy.CONTINUE) {
                    log.error("ChangeSet {} failed, continuing because failure-strategy=CONTINUE", changeSet.id(), ex);
                    continue;
                }
                throw ex;
            }
        }

        log.info("DataChange startup finished (mode={}, strategy={}, dryRun={})",
                properties.getMode().name().toLowerCase(Locale.ROOT),
                properties.getFailureStrategy().name().toLowerCase(Locale.ROOT),
                properties.isDryRun());
    }

    private void validateConfiguration() {
        List<String> errors = new ArrayList<>();

        validateStringList("datachange.locations", properties.getLocations(), true, errors);
        validateStringList("datachange.include-ids", properties.getIncludeIds(), false, errors);
        validateStringList("datachange.exclude-ids", properties.getExcludeIds(), false, errors);
        validateStringList("datachange.include-labels", properties.getIncludeLabels(), false, errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Invalid datachange configuration: " + String.join("; ", errors));
        }
    }

    private static void validateStringList(String propertyName, List<String> values, boolean required, List<String> errors) {
        if (values == null || values.isEmpty()) {
            if (required) {
                errors.add(propertyName + " must not be empty");
            }
            return;
        }

        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (value == null || value.isBlank()) {
                errors.add(propertyName + "[" + i + "] must not be blank");
            }
        }
    }
}

