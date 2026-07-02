package de.graube.datachange.framework.parser;

import de.graube.datachange.framework.model.ChangeSetDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonChangeSetParserTest {

    @Test
    void shouldParseChangeSetAndCreateStableChecksum() {
        JsonChangeSetParser parser = new JsonChangeSetParser(new ObjectMapper());
        String json = """
                {
                  "specVersion": "1.0",
                  "id": "customer-seed-001",
                  "author": "example",
                  "description": "Seed a demo customer",
                  "labels": ["seed"],
                  "tags": ["demo"],
                  "transactionMode": "CHANGESET",
                  "changes": [
                    {
                      "id": "insert-customer",
                      "op": "insert",
                      "entity": "Customer",
                      "values": {
                        "email": "demo@test.de",
                        "status": "ACTIVE"
                      }
                    }
                  ]
                }
                """;

        ChangeSetDefinition changeSet = parser.parse(new ByteArrayResource(json.getBytes()));

        assertNotNull(changeSet);
        assertEquals("customer-seed-001", changeSet.id());
        assertEquals("example", changeSet.author());
        assertEquals("1.0", changeSet.specVersion());
        assertEquals(parser.checksum(changeSet), parser.checksum(changeSet));
        assertEquals(parser.serialize(changeSet), parser.serialize(changeSet));
    }
}

