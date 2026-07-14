package io.github.audipz.datachange.framework.rest;

import io.github.audipz.datachange.framework.api.DataChangeQueryResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataChangeQueryServiceTest {

    @Test
    void shouldProjectSelectedFieldsFromQueryResults() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(
                new SampleCustomer("alice@test.de", "ACTIVE", 31),
                new SampleCustomer("bob@test.de", "INACTIVE", 42)
        ));

        DataChangeQueryService service = new DataChangeQueryService(entityManager);
        DataChangeQueryResult result = service.query("Customer", "email,status", null);

        assertThat(result.entity()).isEqualTo("Customer");
        assertThat(result.count()).isEqualTo(2);
        assertThat(result.fields()).containsExactly("email", "status");
        assertThat(result.rows()).containsExactly(
                Map.of("email", "alice@test.de", "status", "ACTIVE"),
                Map.of("email", "bob@test.de", "status", "INACTIVE")
        );
    }

    @Test
    void shouldRejectEmptyFieldSelection() {
        DataChangeQueryService service = new DataChangeQueryService(mock(EntityManager.class));

        try {
            service.query("Customer", "  ", null);
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).contains("fields is required");
        }
    }

    static class SampleCustomer {
        private final String email;
        private final String status;
        private final int age;

        SampleCustomer(String email, String status, int age) {
            this.email = email;
            this.status = status;
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public String getStatus() {
            return status;
        }

        public int getAge() {
            return age;
        }
    }
}

