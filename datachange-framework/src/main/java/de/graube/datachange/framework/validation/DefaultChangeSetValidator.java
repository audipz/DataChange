package de.graube.datachange.framework.validation;

import de.graube.datachange.framework.model.ChangeDefinition;
import de.graube.datachange.framework.model.ChangeSetDefinition;
import de.graube.datachange.framework.spi.ChangeSetSemanticValidator;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Default semantic validator that checks required ChangeSet fields and basic operation completeness.
 */
@Component
public class DefaultChangeSetValidator implements ChangeSetSemanticValidator {

    @Override
    public List<String> validate(ChangeSetDefinition changeSet) {
        List<String> errors = new ArrayList<>();

        if (changeSet.id() == null || changeSet.id().isBlank()) {
            errors.add("changeset.id is required");
        }
        if (changeSet.author() == null || changeSet.author().isBlank()) {
            errors.add("changeset.author is required");
        }
        if (changeSet.changes() == null || changeSet.changes().isEmpty()) {
            errors.add("changeset.changes must not be empty");
        } else {
            for (ChangeDefinition change : changeSet.changes()) {
                if (change.op() == null || change.op().isBlank()) {
                    errors.add("change.op is required");
                }
                if (change.entity() == null || change.entity().isBlank()) {
                    errors.add("change.entity is required");
                }
            }
        }

        return errors;
    }
}
