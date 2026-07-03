package io.github.audipz.datachange.framework.boot;

import io.github.audipz.datachange.framework.api.ChangeSetSource;
import io.github.audipz.datachange.framework.api.DataChangeExecutionResult;
import io.github.audipz.datachange.framework.api.DataChangeExecutor;
import io.github.audipz.datachange.framework.config.DataChangeFailureStrategy;
import io.github.audipz.datachange.framework.config.DataChangeProperties;
import io.github.audipz.datachange.framework.model.ChangeSetDefinition;
import io.github.audipz.datachange.framework.model.TransactionMode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataChangeStartupRunnerTest {

    @Test
    void shouldFailFastByDefault() {
        ChangeSetSource source = mock(ChangeSetSource.class);
        DataChangeExecutor executor = mock(DataChangeExecutor.class);
        DataChangeProperties properties = new DataChangeProperties();

        ChangeSetDefinition failing = changeSet("cs-fail");
        when(source.load()).thenReturn(List.of(failing));
        doThrow(new IllegalStateException("boom")).when(executor).execute(failing);

        DataChangeStartupRunner runner = new DataChangeStartupRunner(source, executor, properties);

        assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
        verify(executor, times(1)).execute(failing);
    }

    @Test
    void shouldContinueOnFailureWhenConfigured() {
        ChangeSetSource source = mock(ChangeSetSource.class);
        DataChangeExecutor executor = mock(DataChangeExecutor.class);
        DataChangeProperties properties = new DataChangeProperties();
        properties.setFailureStrategy(DataChangeFailureStrategy.CONTINUE);

        ChangeSetDefinition failing = changeSet("cs-fail");
        ChangeSetDefinition succeeding = changeSet("cs-ok");
        when(source.load()).thenReturn(List.of(failing, succeeding));
        doThrow(new IllegalStateException("boom")).when(executor).execute(failing);
        when(executor.execute(succeeding)).thenReturn(new DataChangeExecutionResult(
                "cs-ok",
                DataChangeExecutionResult.Status.SUCCESS,
                1,
                0,
                0,
                Duration.ZERO,
                "executed"
        ));

        DataChangeStartupRunner runner = new DataChangeStartupRunner(source, executor, properties);

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments()));
        verify(executor, times(1)).execute(failing);
        verify(executor, times(1)).execute(succeeding);
    }

    @Test
    void shouldSkipExecutionInDryRunMode() {
        ChangeSetSource source = mock(ChangeSetSource.class);
        DataChangeExecutor executor = mock(DataChangeExecutor.class);
        DataChangeProperties properties = new DataChangeProperties();
        properties.setDryRun(true);

        when(source.load()).thenReturn(List.of(changeSet("cs-a"), changeSet("cs-b")));

        DataChangeStartupRunner runner = new DataChangeStartupRunner(source, executor, properties);

        assertDoesNotThrow(() -> runner.run(new DefaultApplicationArguments()));
        verify(executor, times(0)).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectBlankLocationAtStartup() {
        ChangeSetSource source = mock(ChangeSetSource.class);
        DataChangeExecutor executor = mock(DataChangeExecutor.class);
        DataChangeProperties properties = new DataChangeProperties();
        properties.setLocations(List.of(" "));

        DataChangeStartupRunner runner = new DataChangeStartupRunner(source, executor, properties);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("datachange.locations[0] must not be blank"));
    }

    @Test
    void shouldRejectBlankIncludeIdAtStartup() {
        ChangeSetSource source = mock(ChangeSetSource.class);
        DataChangeExecutor executor = mock(DataChangeExecutor.class);
        DataChangeProperties properties = new DataChangeProperties();
        properties.setIncludeIds(List.of("valid", ""));

        DataChangeStartupRunner runner = new DataChangeStartupRunner(source, executor, properties);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> runner.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("datachange.include-ids[1] must not be blank"));
    }

    private static ChangeSetDefinition changeSet(String id) {
        return new ChangeSetDefinition(
                "1.0",
                id,
                "tester",
                "desc",
                List.of(),
                List.of(),
                TransactionMode.PER_CHANGE,
                null,
                List.of()
        );
    }
}

