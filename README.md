# Azure Service Bus Memory Leak Reproduction Tool

This project is designed to reproduce and investigate memory issues in Azure Service Bus Java SDK by simulating a realistic workload with session-enabled message processing.

## Overview

The tool consists of two main components:
- **NoopProcessor**: A Service Bus processor that consumes messages from a topic subscription with session support
- **RandomSender**: Generates random bursts of messages to simulate real-world traffic patterns

The processor periodically dumps heap snapshots every minute to help analyze memory usage over time.

**Expected behavior**: With the current configuration (30MB heap, 5 senders), the processor typically crashes with `OutOfMemoryError` in approximately **15 minutes** due to memory leak in the Azure Service Bus SDK.

## Prerequisites

1. **Java 21** or higher
2. **Maven 3.x**
3. **Azure Service Bus namespace** with:
   - A **topic** created
   - A **subscription** on that topic with **sessions enabled** (required!)
   - Connection string with appropriate permissions (send/receive)

## Configuration

Edit the `run.sh` script and set the following environment variables:

```bash
export AZURE_SERVICEBUS_CONNECTION_STRING="Endpoint=sb://your-namespace.servicebus.windows.net/;SharedAccessKeyName=...;SharedAccessKey=..."
export AZURE_SERVICEBUS_TOPIC_NAME="your-topic-name"
export AZURE_SERVICEBUS_SUBSCRIPTION_NAME="your-subscription-name"
```

## Usage

Make the script executable:
```bash
chmod +x run.sh
```

Run with default 5 senders:
```bash
./run.sh
```

Run with custom number of senders (e.g., 10):
```bash
./run.sh 10
```

### What happens when you run it?

1. Builds the project (if needed)
2. Starts the processor with memory-constrained JVM settings (30MB heap)
3. Waits 5 seconds for processor initialization
4. Starts N sender processes (default: 5)
5. Processor creates heap dumps every minute
6. Runs until processor crashes with OOM (typically ~15 minutes) or you press Ctrl+C
7. Automatically cleans up all processes on exit

## Architecture

### NoopProcessor
- Uses session-based message processing
- Configured for 200 max concurrent sessions
- 1 concurrent call per session
- 500ms session idle timeout
- Simulates 300ms processing time per message
- Creates heap dumps every minute with timestamp

### RandomSender
- Runs in bursts: sends 0-1000 random messages, then breaks for 1-10 seconds
- Each message assigned a random session ID (from 10,000 possible sessions)
- Runs 100 iterations then exits
- Each sender runs independently

## JVM Configuration

The processor runs with memory-constrained settings to help reproduce memory issues:

- **Heap**: 30MB max (`-Xmx30M`)
- **GC Algorithm**: Serial GC (`-XX:+UseSerialGC`)
- **Heap Dump on OOM**: Enabled (`-XX:+HeapDumpOnOutOfMemoryError`)
- **Exit on OOM**: Enabled (`-XX:+ExitOnOutOfMemoryError`)
- **GC Logging**: Detailed GC logs 

Senders run with 40MB heap to ensure they don't run out of memory.

## Output Files

- **`{timestamp}.gc.log`** - Detailed GC activity log 
- **`{timestamp}.heapdump_{duration}.hprof`** - Periodic heap dumps created every minute

## Stopping the Tool

Press **Ctrl+C** to gracefully shut down all processes. The script will:
1. Catch the signal
2. Forward SIGTERM to all child processes (processor + senders)
3. Wait for clean shutdown
4. Exit

The script also automatically stops all senders when the processor crashes.


## Expected Results

With the default configuration (30MB heap, 5 senders), the processor typically runs for approximately **15 minutes** before exhausting available memory and crashing with an `OutOfMemoryError`.

### Typical Crash Sequence

As the memory leak progresses, you'll observe:
1. Increasing frequency of Full GC events
2. GC pauses become longer
3. Memory no longer recovers after Full GC (heap stays at maximum)
4. Final OutOfMemoryError and heap dump

**Example crash log:**

```
[895.258s][info][gc] GC(7337) Pause Full (Allocation Failure) 28M->28M(29M) 41.676ms
[895.298s][info][gc] GC(7338) Pause Full (Allocation Failure) 28M->28M(29M) 39.819ms
[895.337s][info][gc] GC(7339) Pause Full (Allocation Failure) 28M->28M(29M) 37.752ms
java.lang.OutOfMemoryError: Java heap space
Dumping heap to java_pid22655.hprof ...
Heap dump file created [54889334 bytes in 0.656 secs]
Terminating due to java.lang.OutOfMemoryError: Java heap space
Shutting down all processes...
```

**Key indicators of the memory leak:**
- Multiple consecutive Full GC cycles showing `28M->28M(29M)` (no memory recovered)
- GC happening every ~40ms in the final moments
- Heap remains at maximum capacity despite garbage collection attempts

## Analyzing Results

1. **GC Logs**: Look for increasing frequency of GC events and growing pause times
2. **Heap Dumps**: Use tools like Eclipse MAT or VisualVM to analyze:
   - Open multiple heap dumps in sequence
   - Compare retained heap sizes
   - Look for growing object counts (especially connection/session related)
3. **Memory Trends**: Watch for steady memory growth that doesn't recover after full GC

## License

This is a diagnostic/reproduction tool for internal use.
