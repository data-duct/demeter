package net.dataduct.registry;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.kafka.KafkaConsumerMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class DemeterMonitoring implements AutoCloseable {

  private static DemeterMonitoring INSTANCE;
  private Server server;
  private static PrometheusMeterRegistry prometheusMeterRegistry = buildPrometheusMeterRegistry();

  private DemeterMonitoring(int port) {
    try {
      server = new Server(port);
      ServletContextHandler contextHandler = new ServletContextHandler();
      contextHandler.setContextPath("/");
      server.setHandler(contextHandler);
      MetricsServlet metricsServlet =
          new MetricsServlet(prometheusMeterRegistry.getPrometheusRegistry());
      contextHandler.addServlet(new ServletHolder(metricsServlet), "/monitoring/metrics");
      server.start();
    } catch (Exception e) {
      throw new IllegalStateException("Notrack could not be initialized");
    }
  }

  private static PrometheusMeterRegistry buildPrometheusMeterRegistry() {
    CollectorRegistry registry = CollectorRegistry.defaultRegistry;
    PrometheusMeterRegistry prometheusMeterRegistry =
        new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, registry, Clock.SYSTEM);

    new ClassLoaderMetrics().bindTo(prometheusMeterRegistry);
    new JvmMemoryMetrics().bindTo(prometheusMeterRegistry);
    new JvmGcMetrics().bindTo(prometheusMeterRegistry);
    new ProcessorMetrics().bindTo(prometheusMeterRegistry);
    new JvmThreadMetrics().bindTo(prometheusMeterRegistry);
    new KafkaConsumerMetrics().bindTo(prometheusMeterRegistry);
    return prometheusMeterRegistry;
  }

  public static synchronized void init(int port) {
    if (INSTANCE == null) {
      INSTANCE = new DemeterMonitoring(port);
    }
  }

  public static synchronized void init() {
    if (INSTANCE == null) {
      int port = ThreadLocalRandom.current().nextInt(1000, 100000);
      INSTANCE = new DemeterMonitoring(port);
    }
  }

  static Counter getCounter(String name, String description, List<Tag> extraTags) {
    if (prometheusMeterRegistry != null) {
      Counter counter =
          Counter.builder(name)
              .tags(extraTags)
              .description(description)
              .register(prometheusMeterRegistry);
      return counter;
    } else {
      throw new IllegalStateException("meterRegistry can not be null");
    }
  }

  static Gauge getGauge(String name, String description, Number number, List<Tag> tags) {
    if (prometheusMeterRegistry != null) {
      return Gauge.builder(name, number, Number::longValue)
          .tags(tags)
          .description(description)
          .register(prometheusMeterRegistry);
    } else {
      throw new IllegalStateException("meterRegistry can not be null");
    }
  }

  static Timer getMicrometerTimer(String name, String description, List<Tag> tags) {
    if (prometheusMeterRegistry != null) {
      return Timer.builder(name)
          .tags(tags)
          .description(description)
          .publishPercentiles(0.99)
          .publishPercentileHistogram()
          .register(prometheusMeterRegistry);
    } else {
      throw new IllegalStateException("meterRegistry can not be null");
    }
  }

  @Override
  public void close() {
    try {
      this.server.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
