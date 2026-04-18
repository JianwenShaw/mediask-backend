package me.jianwen.mediask.infra.persistence.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;

class JsonbStringTypeHandlerTest {

    private final JsonbStringTypeHandler handler = new JsonbStringTypeHandler();

    @Test
    void setNonNullParameter_ShouldWriteOtherTypedObject() throws SQLException {
        CapturedSetObject captured = new CapturedSetObject();
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class},
                (proxy, method, args) -> {
                    if ("setObject".equals(method.getName()) && args != null && args.length == 3) {
                        captured.index = (Integer) args[0];
                        captured.value = args[1];
                        captured.targetSqlType = (Integer) args[2];
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });

        handler.setNonNullParameter(statement, 1, "{\"triage_stage\":\"READY\"}", null);

        assertEquals(1, captured.index);
        assertEquals("{\"triage_stage\":\"READY\"}", captured.value);
        assertEquals(Types.OTHER, captured.targetSqlType);
    }

    @Test
    void getNullableResult_ShouldReadStringValue() throws SQLException {
        ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[] {ResultSet.class},
                (proxy, method, args) -> {
                    if ("getString".equals(method.getName())) {
                        return "{\"triage_stage\":\"READY\"}";
                    }
                    return defaultValue(method.getReturnType());
                });

        assertEquals("{\"triage_stage\":\"READY\"}", handler.getNullableResult(resultSet, "triage_snapshot_json"));
        assertEquals("{\"triage_stage\":\"READY\"}", handler.getNullableResult(resultSet, 1));
    }

    private Object defaultValue(Class<?> returnType) {
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class CapturedSetObject {
        private int index;
        private Object value;
        private int targetSqlType;
    }
}
