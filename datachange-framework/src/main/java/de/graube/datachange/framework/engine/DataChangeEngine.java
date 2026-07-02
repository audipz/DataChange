package de.graube.datachange.framework.engine;

import de.graube.datachange.framework.api.DataChangeExecutionResult;
import de.graube.datachange.framework.api.DataChangeExecutor;
import de.graube.datachange.framework.api.ExecutionContext;
import de.graube.datachange.framework.internal.engine.OperationCounters;
import de.graube.datachange.framework.spi.ChangeSetParser;
import de.graube.datachange.framework.spi.ConditionEvaluator;
import de.graube.datachange.framework.log.DataChangeLogService;
import de.graube.datachange.framework.model.ChangeDefinition;
import de.graube.datachange.framework.model.ChangeSetDefinition;
import de.graube.datachange.framework.model.TransactionMode;
import de.graube.datachange.framework.spi.ChangeSetSemanticValidator;
import de.graube.datachange.framework.spi.ConditionProvider;
import de.graube.datachange.framework.spi.OperationHandler;
import de.graube.datachange.framework.spi.OperationOutcome;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Default framework execution engine.
 */
@Component
public class DataChangeEngine implements DataChangeExecutor {

    private static final Logger log = LoggerFactory.getLogger(DataChangeEngine.class);

    private final EntityManager entityManager;
    private final ChangeSetParser parser;
    private final DataChangeLogService logService;
    private final ConditionEvaluator conditionEvaluator;
    private final List<OperationHandler> operationHandlers;
    private final List<ConditionProvider> conditionProviders;
    private final List<ChangeSetSemanticValidator> validators;
    private final TransactionTemplate transactionTemplate;
    private final JpaOperationExecutor jpaOperationExecutor;

    public DataChangeEngine(
            EntityManager entityManager,
            ChangeSetParser parser,
            DataChangeLogService logService,
            ConditionEvaluator conditionEvaluator,
            List<OperationHandler> operationHandlers,
            List<ConditionProvider> conditionProviders,
            List<ChangeSetSemanticValidator> validators,
            PlatformTransactionManager transactionManager,
            JpaOperationExecutor jpaOperationExecutor
    ) {
        this.entityManager = entityManager;
        this.parser = parser;
        this.logService = logService;
        this.conditionEvaluator = conditionEvaluator;
        this.operationHandlers = operationHandlers;
        this.conditionProviders = conditionProviders;
        this.validators = validators;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jpaOperationExecutor = jpaOperationExecutor;
    }

    @Override
    public DataChangeExecutionResult execute(ChangeSetDefinition changeSet) {
        Instant started = Instant.now();
        String checksum = parser.checksum(changeSet);

        validate(changeSet);

        if (logService.isExecuted(changeSet.id(), checksum)) {
            return new DataChangeExecutionResult(
                    changeSet.id(),
                    DataChangeExecutionResult.Status.SKIPPED,
                    0,
                    0,
                    0,
                    Duration.between(started, Instant.now()),
                    "already executed"
            );
        }

        if (!conditionEvaluator.evaluate(changeSet.preConditions(), entityManager, conditionProviders)) {
            return new DataChangeExecutionResult(
                    changeSet.id(),
                    DataChangeExecutionResult.Status.SKIPPED,
                    0,
                    0,
                    0,
                    Duration.between(started, Instant.now()),
                    "precondition is false"
            );
        }

        ExecutionContext context = new ExecutionContext();
        OperationCounters counters = new OperationCounters();

        try {
            if (changeSet.transactionMode() == TransactionMode.PER_CHANGE) {
                for (ChangeDefinition change : changeSet.changes()) {
                    transactionTemplate.executeWithoutResult(status -> executeOne(change, context, counters));
                }
            } else {
                transactionTemplate.executeWithoutResult(status -> {
                    for (ChangeDefinition change : changeSet.changes()) {
                        executeOne(change, context, counters);
                    }
                });
            }

            transactionTemplate.executeWithoutResult(status -> 
                    logService.markSuccess(changeSet.id(), checksum, changeSet.author(), started, 
                            counters.inserts, counters.updates, counters.deletes));

            return new DataChangeExecutionResult(
                    changeSet.id(),
                    DataChangeExecutionResult.Status.SUCCESS,
                    counters.inserts,
                    counters.updates,
                    counters.deletes,
                    Duration.between(started, Instant.now()),
                    "executed"
            );
        } catch (RuntimeException ex) {
            log.error("ChangeSet {} failed", changeSet.id(), ex);
            String errorMessage = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            transactionTemplate.executeWithoutResult(status -> logService.markFailed(changeSet.id(), checksum, changeSet.author(), started, errorMessage));
            throw ex;
        }
    }

    private void validate(ChangeSetDefinition changeSet) {
        for (ChangeSetSemanticValidator validator : validators) {
            List<String> errors = validator.validate(changeSet);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Invalid changeset " + changeSet.id() + ": " + String.join(", ", errors));
            }
        }
    }

    private void executeOne(ChangeDefinition change, ExecutionContext context, OperationCounters counters) {
        OperationOutcome outcome = dispatch(change, context);
        switch (outcome) {
            case INSERT -> counters.inserts++;
            case UPDATE -> counters.updates++;
            case DELETE -> counters.deletes++;
            default -> {
            }
        }
    }

    private OperationOutcome dispatch(ChangeDefinition change, ExecutionContext context) {
        for (OperationHandler handler : operationHandlers) {
            if (handler.supports(change.op())) {
                return handler.execute(change, entityManager, context);
            }
        }
        return jpaOperationExecutor.execute(change, entityManager, context);
    }

}

