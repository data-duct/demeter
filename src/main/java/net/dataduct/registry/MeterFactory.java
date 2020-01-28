package net.dataduct.registry;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;

import java.util.*;

public class MeterFactory {

    private static final Map<String, Meter> meters = new HashMap<>();
    private static final Object METERS_LOCK = new Object();

    public static Counter getCounter(String name) {
        return getCounter(name, false);
    }

    public static Counter getCounter(String name, boolean isThreadUnique, Vtag... extraTags) {
        List<Tag> tags = new ArrayList<>();
        if (extraTags != null) tags = Vtag.toTags(Arrays.asList(extraTags));
        return getCounter(name, isThreadUnique, tags);
    }

    public static Counter getCounter(String name, boolean isThreadUnique, List<Tag> contextualTags) {

        String nameWithTags = getName(name, isThreadUnique, contextualTags);

        Counter counter = (Counter) meters.get(nameWithTags);

        if (counter == null) {
            synchronized (METERS_LOCK) {
                counter = DemeterMonitoring.getCounter(name, "", contextualTags);
                meters.put(nameWithTags, counter);
            }
        }
        return counter;
    }

    public static Gauge getGauge(String name, Number value, Vtag... contextualTags) {
        List<Tag> tags = new ArrayList<>();
        if (contextualTags != null) tags = Vtag.toTags(Arrays.asList(contextualTags));
        return getGauge(name, value, tags);
    }

    public static Gauge getGauge(String name, Number value, List<Tag> contextualTags) {

        String nameWithTags = getName(name, false, contextualTags);

        Gauge gauge = (Gauge) meters.get(nameWithTags);
        if (gauge == null) {
            synchronized (METERS_LOCK) {
                gauge = DemeterMonitoring.getGauge(name, "", value, contextualTags);
                meters.put(nameWithTags, gauge);
            }
        }
        return gauge;
    }

    public static io.micrometer.core.instrument.Timer getMicrometerTimer(String name, Vtag... vtags) {

        List<Tag> tags = new ArrayList<>();
        if (vtags != null) tags = Vtag.toTags(Arrays.asList(vtags));

        return getMicrometerTimer(name, tags);
    }

    public static io.micrometer.core.instrument.Timer getMicrometerTimer(
            String name, List<Tag> contextualTags) {

        String nameWithTags = getName(name, false, contextualTags);

        io.micrometer.core.instrument.Timer timer =
                (io.micrometer.core.instrument.Timer) meters.get(nameWithTags);
        if (timer == null) {
            synchronized (METERS_LOCK) {
                timer = DemeterMonitoring.getMicrometerTimer(name, "", contextualTags);
                meters.put(nameWithTags, timer);
            }
        }
        return timer;
    }

    private static String getName(String metricName, boolean thread, List<Tag> tags) {
        if (thread)
            return String.format("%s-%d-%d", metricName, Thread.currentThread().getId(), tags.hashCode());
        return String.format("%s-%d", metricName, tags.hashCode());
    }
}
