package io.github.audipz.datachange.framework.rest;

import io.github.audipz.datachange.framework.api.ChangeSetSource;
import io.github.audipz.datachange.framework.api.DataChangeExecutionResult;
import io.github.audipz.datachange.framework.api.DataChangeExecutor;
import io.github.audipz.datachange.framework.model.ChangeSetDefinition;
import io.github.audipz.datachange.framework.model.TransactionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DataChangeControllerTest {

    private MockMvc mockMvc;

    private ChangeSetSource source;

    private DataChangeExecutor executor;

    @BeforeEach
    void setUp() {
        source = mock(ChangeSetSource.class);
        executor = mock(DataChangeExecutor.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DataChangeController(source, executor))
                .setControllerAdvice(new DataChangeRestExceptionHandler())
                .build();
    }

    @Test
    void shouldListChangesetsAndExecuteDeployedChangeset() throws Exception {
        when(source.load()).thenReturn(List.of(
                new ChangeSetDefinition("1", "cs-1", "dev", "demo", List.of(), List.of(), TransactionMode.PER_CHANGE, null, List.of())
        ));
        when(executor.execute(any())).thenReturn(new DataChangeExecutionResult(
                "cs-1",
                DataChangeExecutionResult.Status.SUCCESS,
                2,
                1,
                0,
                java.time.Duration.ofMillis(245),
                "executed"
        ));

        mockMvc.perform(get("/datachange/changesets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("cs-1"))
                .andExpect(jsonPath("$[0].author").value("dev"));

        mockMvc.perform(post("/datachange/execute").param("id", "cs-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changeSetId").value("cs-1"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.inserts").value(2))
                .andExpect(jsonPath("$.updates").value(1))
                .andExpect(jsonPath("$.deletes").value(0))
                .andExpect(jsonPath("$.message").value("executed"));
    }

    @Test
    void shouldReturnBadRequestForUnknownChangeSet() throws Exception {
        when(source.load()).thenReturn(List.of());

        mockMvc.perform(post("/datachange/execute").param("id", "missing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("ChangeSet not found: missing"));
    }
}

