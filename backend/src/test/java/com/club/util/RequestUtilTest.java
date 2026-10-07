package com.club.util;

import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class RequestUtilTest {
    @Test public void idsMustBePositiveExactIntegers() {
        assertEquals(7L, RequestUtil.id(JsonParser.parseString("{\"id\":7}").getAsJsonObject(), "id"));
        for (String value : new String[]{"null", "0", "-1", "1.5", "\"2\"", "9223372036854775808", "[]"}) {
            var body = JsonParser.parseString("{\"id\":" + value + "}").getAsJsonObject();
            assertThrows(IllegalArgumentException.class, () -> RequestUtil.id(body, "id"));
        }
    }
    @Test public void stringsCannotBeArraysOrNumbers() {
        var body = JsonParser.parseString("{\"name\":42}").getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () -> RequestUtil.string(body, "name"));
        assertNull(RequestUtil.string(body, "missing"));
    }
    @Test public void paginationCannotOverflow() {
        assertEquals(10, RequestUtil.offset(2, 10));
        assertThrows(IllegalArgumentException.class, () -> RequestUtil.offset(Integer.MAX_VALUE, 100));
    }
}
