package io.github.audipz.datachange.example;

import io.github.audipz.datachange.framework.api.ChangeSetSource;
import io.github.audipz.datachange.framework.api.DataChangeExecutionResult;
import io.github.audipz.datachange.framework.api.DataChangeExecutor;
import io.github.audipz.datachange.framework.model.ChangeSetDefinition;
import jakarta.persistence.EntityManager;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void startupRunCreatesCustomerAndSecondRunIsSkipped() {
        assertThat(customerRepository.findByEmail("demo@test.de")).isPresent();

        List<ChangeSetDefinition> loaded = changeSetSource.load();
        assertThat(loaded).hasSizeGreaterThanOrEqualTo(1);

        // Re-execute first changeset: should be skipped (idempotency)
        DataChangeExecutionResult secondRun = dataChangeExecutor.execute(loaded.get(0));
        assertThat(secondRun.status()).isEqualTo(DataChangeExecutionResult.Status.SKIPPED);
    }

    @Test
    void relationshipReAssignChangesetMovesOwningSideRelationsToTargetPerson() {
        String profileOwner = entityManager.createQuery(
                        "select p.person.firstName from PersonProfile p where p.preferredLanguage = :language",
                        String.class)
                .setParameter("language", "de")
                .getSingleResult();
        assertThat(profileOwner).isEqualTo("Erika");

        String officeAddressOwner = entityManager.createQuery(
                        "select a.person.firstName from Address a where a.street = :street",
                        String.class)
                .setParameter("street", "Kontorweg 10")
                .getSingleResult();
        assertThat(officeAddressOwner).isEqualTo("Erika");

        String bankAccountOwner = entityManager.createQuery(
                        "select b.person.firstName from BankAccount b where b.iban = :iban",
                        String.class)
                .setParameter("iban", "DE02120300000000202051")
                .getSingleResult();
        assertThat(bankAccountOwner).isEqualTo("Erika");

        String homeAddressOwner = entityManager.createQuery(
                        "select a.person.firstName from Address a where a.street = :street",
                        String.class)
                .setParameter("street", "Hauptstrasse 1")
                .getSingleResult();
        assertThat(homeAddressOwner).isEqualTo("Max");
    }

    @Test
    void customerCrudChangesetAppliesCreateReadUpdateDeleteAsExpected() {
        Customer createdAndUpdated = customerRepository.findByEmail("crud-create@test.de").orElseThrow();
        assertThat(createdAndUpdated.getStatus()).isEqualTo(CustomerStatus.INACTIVE);

        assertThat(customerRepository.findByEmail("crud-delete@test.de")).isEmpty();

        Customer upserted = customerRepository.findByEmail("crud-upsert@test.de").orElseThrow();
        assertThat(upserted.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}
