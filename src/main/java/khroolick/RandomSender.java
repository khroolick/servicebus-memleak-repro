package khroolick;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import java.time.Duration;
import java.util.Random;

public class RandomSender {
    Random r = new Random();
    ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
            .connectionString(System.getenv("AZURE_SERVICEBUS_CONNECTION_STRING"))
            .sender()
            .topicName(System.getenv("AZURE_SERVICEBUS_TOPIC_NAME"))
            .buildClient();

    public static void main(String[] args) throws InterruptedException {
        new RandomSender().start();
    }

    private void start() throws InterruptedException {
        for (int runId = 0; runId < 100; runId++) {
            int messagesQty = r.nextInt(1000);
            int breakInSeconds = r.nextInt(10) + 1;

            System.out.format("Sending %d messages, then break for %d seconds (runId=%d)%n", messagesQty, breakInSeconds, runId);
            for (int messageId = 0; messageId < messagesQty; messageId++) {
                ServiceBusMessage message = new ServiceBusMessage("Hello!, runId=%d, msgId=%d".formatted(runId, messageId));
                message.setSessionId(getRandomSession());
                senderClient.sendMessage(message);
            }
            Thread.sleep(Duration.ofSeconds(breakInSeconds));
        }
    }

    private String getRandomSession() {
        return "%05d".formatted((r.nextInt(10000) + 1));
    }
}
