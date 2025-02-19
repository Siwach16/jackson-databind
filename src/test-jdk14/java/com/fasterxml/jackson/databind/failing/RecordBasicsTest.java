package com.fasterxml.jackson.databind.failing;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.ClassUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// 01-Dec-2022, tatu: Alas, fails on JDK 17
public class RecordBasicsTest extends BaseMapTest
{
    record EmptyRecord() { }

    record SimpleRecord(int id, String name) { }

    record RecordOfRecord(SimpleRecord record) { }

    record RecordWithIgnore(int id, @JsonIgnore String name) { }

    record RecordWithRename(int id, @JsonProperty("rename")String name) { }

    // [databind#2992]
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record SnakeRecord(String myId, String myValue){}

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************************
    /* Test methods, Record type introspection
    /**********************************************************************
     */

    public void testClassUtil() {
        assertFalse(ClassUtil.isRecordType(getClass()));

        assertTrue(ClassUtil.isRecordType(SimpleRecord.class));
        assertTrue(ClassUtil.isRecordType(RecordOfRecord.class));
        assertTrue(ClassUtil.isRecordType(RecordWithIgnore.class));
        assertTrue(ClassUtil.isRecordType(RecordWithRename.class));
    }

    public void testRecordJavaType() {
        assertFalse(MAPPER.constructType(getClass()).isRecordType());

        assertTrue(MAPPER.constructType(SimpleRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordOfRecord.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordWithIgnore.class).isRecordType());
        assertTrue(MAPPER.constructType(RecordWithRename.class).isRecordType());
    }

    /*
    /**********************************************************************
    /* Test methods, default reading/writing Record values
    /**********************************************************************
     */

    public void testSerializeSimpleRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new SimpleRecord(123, "Bob"));
        final Object EXP = map("id", Integer.valueOf(123), "name", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    public void testDeserializeSimpleRecord() throws Exception {
        assertEquals(new SimpleRecord(123, "Bob"),
                MAPPER.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class));
    }

    public void testSerializeEmptyRecord() throws Exception {
        assertEquals("{}", MAPPER.writeValueAsString(new EmptyRecord()));
    }

    public void testDeserializeEmptyRecord() throws Exception {
        assertEquals(new EmptyRecord(),
                MAPPER.readValue("{}", EmptyRecord.class));
    }

    public void testSerializeRecordOfRecord() throws Exception {
        RecordOfRecord record = new RecordOfRecord(new SimpleRecord(123, "Bob"));
        String json = MAPPER.writeValueAsString(record);
        final Object EXP = Collections.singletonMap("record",
                map("id", Integer.valueOf(123), "name", "Bob"));
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    /*
    /**********************************************************************
    /* Test methods, reading/writing Record values with different config
    /**********************************************************************
     */

    public void testSerializeSimpleRecord_DisableAnnotationIntrospector() throws Exception {
        SimpleRecord record = new SimpleRecord(123, "Bob");

        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        String json = mapper.writeValueAsString(record);

        assertEquals("{\"id\":123,\"name\":\"Bob\"}", json);
    }

    public void testDeserializeSimpleRecord_DisableAnnotationIntrospector() throws Exception {
        JsonMapper mapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        SimpleRecord value = mapper.readValue("{\"id\":123,\"name\":\"Bob\"}", SimpleRecord.class);

        assertEquals(new SimpleRecord(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, ignorals, renames
    /**********************************************************************
     */

    public void testSerializeJsonIgnoreRecord() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithIgnore(123, "Bob"));
        assertEquals("{\"id\":123}", json);
    }

    public void testSerializeJsonRename() throws Exception {
        String json = MAPPER.writeValueAsString(new RecordWithRename(123, "Bob"));
        final Object EXP = map("id", Integer.valueOf(123), "rename", "Bob");
        assertEquals(EXP, MAPPER.readValue(json, Object.class));
    }

    public void testDeserializeJsonRename() throws Exception {
        RecordWithRename value = MAPPER.readValue("{\"id\":123,\"rename\":\"Bob\"}",
                RecordWithRename.class);
        assertEquals(new RecordWithRename(123, "Bob"), value);
    }

    /*
    /**********************************************************************
    /* Test methods, naming strategy
    /**********************************************************************
     */

    // [databind#2992]
    public void testNamingStrategy() throws Exception
    {
        SnakeRecord input = new SnakeRecord("123", "value");
        String json = MAPPER.writeValueAsString(input);
        SnakeRecord output = MAPPER.readValue(json, SnakeRecord.class);
        assertEquals(input, output);
    }

    /*
    /**********************************************************************
    /* Internal helper methods
    /**********************************************************************
     */

    private Map<String,Object> map(String key1, Object value1,
            String key2, Object value2) {
        final Map<String, Object> result = new LinkedHashMap<>();
        result.put(key1, value1);
        result.put(key2, value2);
        return result;
    }
}
