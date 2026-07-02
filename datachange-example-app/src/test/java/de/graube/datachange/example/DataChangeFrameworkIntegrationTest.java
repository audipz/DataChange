package de.graube.datachange.example;

import de.graube.datachange.framework.api.ChangeSetSource;
import de.graube.datachange.framework.api.DataChangeExecutionResult;
import de.graube.datachange.framework.api.DataChangeExecutor;
import de.graube.datachange.framework.model.ChangeSetDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataChangeFrameworkIntegrationTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DataChangeExecutor dataChangeExecutor;

    @Autowired
    private ChangeSetSource changeSetSource;

    @Test
    void startupRunCreatesCustomerAndSecondRunIsSkipped() {
        assertThat(customerRepository.findByEmail("demo@test.de")).isPresent();

        List<ChangeSetDefinition> loaded = changeSetSource.load();
        assertThat(loaded).hasSizeGreaterThanOrEqualTo(1);

        // Re-execute first changeset: should be skipped (idempotency)
        DataChangeExecutionResult secondRun = dataChangeExecutor.execute(loaded.get(0));
        assertThat(secondRun.status()).isEqualTo(DataChangeExecutionResult.Status.SKIPPED);
    }
}

