package io.github.audipz.datachange.framework.internal.engine;

import io.github.audipz.datachange.framework.api.ExecutionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationValueResolverLookupTest {

    @Test
    void resolvesLookupPlaceholderToSingleFieldValue() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);

        when(entityManager.createQuery("select e.status from LookupCustomer e where e.email = :whereValue")).thenReturn(query);
        when(query.setParameter("whereValue", "lookup@test.de")).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of("INACTIVE"));

        Object resolved = OperationValueResolver.resolveValue(
                "${lookup('LookupCustomer','email','lookup@test.de','status')}",
                new ExecutionContext(),
                entityManager
        );

        assertThat(resolved).isEqualTo("INACTIVE");
    }

    @Test
    void resolvesLookupPlaceholderWithNumericWhereValue() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);

        when(entityManager.createQuery("select e.lastName from Person e where e.age = :whereValue")).thenReturn(query);
        when(query.setParameter("whereValue", 31)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of("Musterfrau"));

        Object resolved = OperationValueResolver.resolveValue(
                "${lookup('Person','age',31,'lastName')}",
                new ExecutionContext(),
                entityManager
        );

        assertThat(resolved).isEqualTo("Musterfrau");
    }

    @Test
    void resolvesLookupPlaceholderWithComplexWhereExpression() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);

        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(query.setParameter("p1", 18L)).thenReturn(query);
        when(query.setParameter("p2", 65L)).thenReturn(query);
        when(query.setParameter("p3", Boolean.TRUE)).thenReturn(query);
        when(query.setParameter("p4", Boolean.TRUE)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(42L));

        Object resolved = OperationValueResolver.resolveValue(
                "${lookup('Person',\"age >= 18 and age <= 65 and (active == true or vip == true)\",'id')}",
                new ExecutionContext(),
                entityManager
        );

        assertThat(resolved).isEqualTo(42L);
    }

    @Test
    void failsWhenLookupHasNoMatches() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);

        when(entityManager.createQuery("select e.status from LookupCustomer e where e.email = :whereValue")).thenReturn(query);
        when(query.setParameter("whereValue", "missing@test.de")).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThatThrownBy(() -> OperationValueResolver.resolveValue(
                "${lookup('LookupCustomer','email','missing@test.de','status')}",
                new ExecutionContext(),
                entityManager
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lookup returned no rows");
    }
}
