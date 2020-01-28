package net.dataduct.agent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;

public class CountInterceptor {
  @RuntimeType
  public static Object intercept(@Origin Method method, @SuperCall Callable<?> callable) {
    System.out.println("Count intercepter ran");
    Annotation[][] annotations = method.getParameterAnnotations();
    System.out.println(annotations.length);
    System.out.println(annotations[0]);
    return null;
  }
}
