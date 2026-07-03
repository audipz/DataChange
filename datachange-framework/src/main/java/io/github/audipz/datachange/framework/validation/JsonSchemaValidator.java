package io.github.audipz.datachange.framework.validation;

import io.github.audipz.datachange.framework.model.ChangeDefinition;
import io.github.audipz.datachange.framework.model.ChangeSetDefinition;
import io.github.audipz.datachange.framework.spi.ChangeSetSemanticValidator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Validates ChangeSets against the framework DSL rules that are encoded as schema-like constraints.
 */
@Component
public class JsonSchemaValidator implements ChangeSetSemanticValidator {

    @Override
    public List<String> validate(ChangeSetDefinition changeSet) {
        List<String> errors = new ArrayList<>();

        // Required fields
        if (changeSet.id() == null || changeSet.id().isBlank()) {
            errors.add("id is required");
        } else if (changeSet.id().length() > 256) {
            errors.add("id must be <= 256 characters");
        }

        if (changeSet.author() == null || changeSet.author().isBlank()) {
            errors.add("author is required");
        } else if (changeSet.author().length() > 256) {
            errors.add("author must be <= 256 characters");
        }

        if (changeSet.description() == null || changeSet.description().isBlank()) {
            errors.add("description is required");
        }

        if (changeSet.transactionMode() == null) {
            errors.add("transactionMode is required");
        }

        if (changeSet.changes() == null || changeSet.changes().isEmpty()) {
            errors.add("changes must contain at least one operation");
        } else {
            for (int i = 0; i < changeSet.changes().size(); i++) {
                ChangeDefinition change = changeSet.changes().get(i);
                String prefix = "changes[" + i + "].";

                if (change.id() == null || change.id().isBlank()) {
                    errors.add(prefix + "id is required");
                }

                if (change.op() == null || change.op().isBlank()) {
                    errors.add(prefix + "op is required");
                } else {
                    String op = change.op().toLowerCase(Locale.ROOT);
                    if (!List.of("insert", "update", "delete", "upsert", "merge").contains(op)) {
                        errors.add(prefix + "op must be one of: insert, update, delete, upsert, merge");
                    }
                }

                if (change.entity() == null || change.entity().isBlank()) {
                    errors.add(prefix + "entity is required");
                }
            }
        }

        return errors;
    }
}
