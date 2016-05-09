package pl.allegro.tech.hermes.consumers.consumer.status;

public class Status {
    private final StatusType statusType;
    private final long timestamp;

    public Status(StatusType statusType, long timestamp) {
        this.statusType = statusType;
        this.timestamp = timestamp;
    }

    public StatusType getType() {
        return statusType;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static enum StatusType {
        NEW, STARTING, STARTED, CONSUMING, STOPPING, STOPPED, BROKEN
    }
}
