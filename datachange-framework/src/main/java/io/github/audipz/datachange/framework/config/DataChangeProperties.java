package io.github.audipz.datachange.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runtime configuration for datachange framework.
 */
@ConfigurationProperties(prefix = "datachange")
@Validated
public class DataChangeProperties {

    private boolean enabled = true;
    @NotNull
    private DataChangeMode mode = DataChangeMode.STARTUP;
    @NotNull
    private DataChangeFailureStrategy failureStrategy = DataChangeFailureStrategy.FAIL_FAST;
    private boolean dryRun = false;
    @NotEmpty
    private List<String> locations = new ArrayList<>(List.of("classpath*:datachange/*.json"));
    private List<String> includeIds = new ArrayList<>();
    private List<String> excludeIds = new ArrayList<>();
    private List<String> includeLabels = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataChangeMode getMode() {
        return mode;
    }

    public void setMode(DataChangeMode mode) {
        this.mode = mode;
    }

    public DataChangeFailureStrategy getFailureStrategy() {
        return failureStrategy;
    }

    public void setFailureStrategy(DataChangeFailureStrategy failureStrategy) {
        this.failureStrategy = failureStrategy;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations == null ? new ArrayList<>() : new ArrayList<>(locations);
    }

    public List<String> getIncludeIds() {
        return includeIds;
    }

    public void setIncludeIds(List<String> includeIds) {
        this.includeIds = includeIds == null ? new ArrayList<>() : new ArrayList<>(includeIds);
    }

    public List<String> getExcludeIds() {
        return excludeIds;
    }

    public void setExcludeIds(List<String> excludeIds) {
        this.excludeIds = excludeIds == null ? new ArrayList<>() : new ArrayList<>(excludeIds);
    }

    public List<String> getIncludeLabels() {
        return includeLabels;
    }

    public void setIncludeLabels(List<String> includeLabels) {
        this.includeLabels = includeLabels == null ? new ArrayList<>() : new ArrayList<>(includeLabels);
    }

    @AssertTrue(message = "datachange.include-ids and datachange.exclude-ids must not overlap")
    public boolean isIdFilterCombinationValid() {
        return Collections.disjoint(includeIds, excludeIds);
    }
}

