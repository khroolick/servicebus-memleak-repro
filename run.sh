#!/bin/bash

# Set required Azure Service Bus environment variables
export AZURE_SERVICEBUS_CONNECTION_STRING="Endpoint=sb://your_ns.servicebus.windows.net/;SharedAccessKeyName..."
export AZURE_SERVICEBUS_TOPIC_NAME="your_topic"
export AZURE_SERVICEBUS_SUBSCRIPTION_NAME="your_sub"

# Number of senders (default to 5 if not provided)
NUM_SENDERS=${1:-5}

# Generate current date for log file naming
current_date=$(date +%Y%m%d_%H%M%S)

# JVM options for processor
JVM_OPTS="-Xmx30M"
JVM_OPTS="$JVM_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JVM_OPTS="$JVM_OPTS -XX:+ExitOnOutOfMemoryError"
JVM_OPTS="$JVM_OPTS -Xlog:gc=info"
JVM_OPTS="$JVM_OPTS -Xlog:gc*=debug:file=${current_date}.gc.log:time,uptime,level,tags:filesize=1024M"
JVM_OPTS="$JVM_OPTS -XX:+UseSerialGC"

# Array to store child PIDs
CHILD_PIDS=()

# Cleanup function to kill all child processes
cleanup() {
    echo "Shutting down all processes...${CHILD_PIDS[@]}"
    for pid in "${CHILD_PIDS[@]}"; do
        kill "$pid" 2>/dev/null
    done
    wait
    exit 0
}

# Trap SIGINT (Ctrl+C) and SIGTERM
trap cleanup SIGINT SIGTERM

# Build the project
#echo "Building project..."
#mvn clean compile

# Start processor in background
echo "Starting processor..."
MAVEN_OPTS="$JVM_OPTS" mvn -q exec:java -Dexec.mainClass=khroolick.NoopProcessor -Dexec.arguments="${current_date}" &
PROCESSOR_PID=$!
CHILD_PIDS+=($PROCESSOR_PID)
echo "Processor started with PID: $PROCESSOR_PID"

# Wait for processor to initialize
sleep 5

# Start N senders
echo "Starting $NUM_SENDERS senders..."
for i in $(seq 1 $NUM_SENDERS); do
    MAVEN_OPTS="-Xmx40M" mvn -q exec:java -Dexec.mainClass=khroolick.RandomSender &
    SENDER_PID=$!
    CHILD_PIDS+=($SENDER_PID)
    echo "Sender $i started with PID: $SENDER_PID"
done

echo "All processes started. Press Ctrl+C to stop all."

# Wait for processor to crash
wait $PROCESSOR_PID
cleanup

