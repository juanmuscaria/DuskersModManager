package com.juanmuscaria.tooling.dmm;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

public class UnsafeJvm {
    public static final MethodHandles.Lookup theLookup;

    static {
        try {
            var unsafeClass = Class.forName("sun.misc.Unsafe");
            Object unsafe = null;
            for (Field field : unsafeClass.getDeclaredFields()) {
                if (field.getType().equals(unsafeClass)) {
                    field.setAccessible(true);
                    unsafe = field.get(null);
                }
            }
            MethodHandles.lookup();
            var lookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            var lookupFieldOffset = (long) unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class)
                .invoke(unsafe, lookupField);

            theLookup = (MethodHandles.Lookup) unsafeClass.getDeclaredMethod("getObject", Object.class, long.class)
                .invoke(unsafe, MethodHandles.Lookup.class, lookupFieldOffset);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
