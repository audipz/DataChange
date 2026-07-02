package de.graube.datachange.framework.boot;

import de.graube.datachange.framework.config.DataChangeProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration entry point for the framework.
 */
@AutoConfiguration
@EnableConfigurationProperties(DataChangeProperties.class)
public class DataChangeAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "datachange", name = "enabled", havingValue = "true", matchIfMissing = true)
    DataChangeStartupRunner dataChangeStartupRunner(
            de.graube.datachange.framework.api.ChangeSetSource source,
            de.graube.datachange.framework.api.DataChangeExecutor executor,
            DataChangeProperties properties
    ) {
        return new DataChangeStartupRunner(source, executor, properties);
    }
}

