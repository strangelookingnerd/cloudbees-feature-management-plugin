package io.rollout.publicapi.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.time.Instant;
import junit.framework.TestCase;
import org.junit.Test;

public class AuditLogTest extends TestCase {
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Test
    public void testDeserialize() throws JsonProcessingException {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String json = "{"
                + "  \"_id\": \"abc\","
                + "  \"application\": \"def\","
                + "  \"experiment\": \"ghi\","
                + "  \"user\": {"
                + "    \"_id\": \"userid\","
                + "    \"email\": \"foo@bar.com\","
                + "    \"name\": \"Foo Bar\","
                + "    \"picture\": \"https://piccy.mc/picface\""
                + "  },"
                + "  \"userName\": \"Foo Bar\","
                + "  \"userEmail\": \"foo@bar.com\","
                + "  \"action\": \"stations\","
                + "  \"message\": \"In a Bottle\","
                + "  \"environmentId\": \"chilly\","
                + "  \"creation_date\": \"2022-01-14T14:55:41.777Z\","
                + "  \"__v\": 0"
                + "}";

        AuditLog log = mapper.readValue(json, AuditLog.class);

        assertEquals("Foo Bar", log.getUserName());
        assertEquals("foo@bar.com", log.getUserEmail());
        assertEquals("In a Bottle", log.getMessage());
        assertEquals("stations", log.getAction());
        assertEquals("https://piccy.mc/picface", log.getUser().getPicture());
        assertEquals(Date.from(Instant.parse("2022-01-14T14:55:41.777Z")), log.getCreationDate());
    }
}
