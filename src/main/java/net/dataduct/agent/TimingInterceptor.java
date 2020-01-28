package net.dataduct.agent;


import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.dataduct.registry.MeterFactory;

public class TimingInterceptor {
    @RuntimeType
    public static Object intercept(@Origin Method method, @SuperCall Callable<?> callable) {
        long start = System.currentTimeMillis();
        try {
            return callable.call();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            System.out.println(method + " took " + (System.currentTimeMillis() - start));
            long time = System.currentTimeMillis() - start;
            String className = method.getDeclaringClass().getName();

            MeterFactory.getMicrometerTimer(String.format("%s.%s", className, method.getName()))
                    .record(time, TimeUnit.MILLISECONDS);
        }
    }
}
