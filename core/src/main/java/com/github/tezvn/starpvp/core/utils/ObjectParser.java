package com.github.tezvn.starpvp.core.utils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.UUID;

public class ObjectParser {

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Enum<?>> T parseEnum(Class<?> wrapper, String value) {
        if(value == null)
            return null;
        try {
            return (T) wrapper.getMethod("valueOf", String.class).invoke(null, value);
        }catch (Exception e) {
            return null;
        }
    }

    public static BigDecimal parseNumber(String value) {
        return new BigDecimal(value);
    }

    public static UUID parseUUID(String value) {
        try {
            if(value == null)
                return null;
            return UUID.fromString(value);
        }catch (Exception e) {
            return null;
        }
    }
}
