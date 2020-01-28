package net.dataduct.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.concurrent.TimeUnit;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import net.dataduct.annotation.Counter;
import net.dataduct.annotation.Monitor;
import net.dataduct.registry.MeterFactory;

public class TimerAgent {
  public static void premain(String arguments, Instrumentation instrumentation) {
    System.out.println("premain running");
    new AgentBuilder.Default()
        .type(ElementMatchers.isAnnotatedWith(Monitor.class))
        .transform(new MetricsTransformer())
        .installOn(instrumentation);
  }

  private static class MetricsTransformer implements AgentBuilder.Transformer {
    @Override
    public DynamicType.Builder<?> transform(
        final DynamicType.Builder<?> builder,
        final TypeDescription typeDescription,
        final ClassLoader classLoader,
        final JavaModule module) {

      final AsmVisitorWrapper methodsVisitor =
          Advice.to(EnterAdvice.class, ExitAdviceMethods.class).on(ElementMatchers.isMethod());

      return builder.visit(methodsVisitor);
    }

    private static class EnterAdvice {
      @Advice.OnMethodEnter
      static long enter(@Advice.Origin final Executable executable) {
        long time = System.nanoTime();
        Parameter[] parameters = executable.getParameters();
        for (Parameter parameter : parameters) {
          if (parameter.isAnnotationPresent(Counter.class)) {
            String counterName = parameter.getAnnotation(Counter.class).name();
            MeterFactory.getCounter(counterName).increment();
          }
        }

        System.out.println(executable.getDeclaringClass().getName() + " " + executable.getName());
        return time;
      }
    }

    private static class ExitAdviceMethods {
      @Advice.OnMethodExit(onThrowable = Throwable.class)
      static void exit(
          @Advice.Origin final Executable executable,
          @Advice.Enter final long startTime,
          @Advice.Thrown final Throwable throwable) {
        final long duration = System.nanoTime() - startTime;
        String className = executable.getDeclaringClass().getName();
        MeterFactory.getMicrometerTimer(String.format("%s.%s", className, executable.getName()))
            .record(duration, TimeUnit.NANOSECONDS);
      }
    }
  }
}
