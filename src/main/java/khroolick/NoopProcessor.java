package khroolick;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.sun.management.HotSpotDiagnosticMXBean;

import javax.management.MBeanServer;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;

public class NoopProcessor {
    public static void main(String[] args) {
        Instant started = Instant.now();
        String connectionString = System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING");
        String topicName = System.getenv("AZURE_SERVICEBUS_TOPIC_NAME");
        String subscriptionName = System.getenv("AZURE_SERVICEBUS_SUBSCRIPTION_NAME");

        ServiceBusProcessorClient processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sessionProcessor()
                .maxConcurrentSessions(200)
                .maxConcurrentCalls(1)
                .sessionIdleTimeout(Duration.ofMillis(500))
                .topicName(topicName)
                .subscriptionName(subscriptionName)
                .processMessage(m -> {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                })
                .processError(e -> e.getException().printStackTrace())
                .buildProcessorClient();

        processorClient.start();
        System.out.println("azure-core-amqp loaded from: " +
                com.azure.core.amqp.implementation.ReactorConnection.class
                        .getProtectionDomain().getCodeSource().getLocation());

        try {
            while (true) {
                dumpHeap(String.format("%s.heapdump_%s.hprof", args[0], Duration.between(started, Instant.now())), true);
                Thread.sleep(Duration.ofMinutes(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            processorClient.close();
        }
    }

    public static void dumpHeap(String filePath, boolean live) throws IOException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        mxBean.dumpHeap(filePath, live);
    }
}
