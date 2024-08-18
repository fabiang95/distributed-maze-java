package message;

import java.io.Serializable;

public class BaseMessage implements Comparable<BaseMessage>, Serializable {
    public final MessageType messageType;
    public final int priority;
    private final PlayerBasicInfo senderInfo;
    public BaseMessage(MessageType MessageType, int priority, PlayerBasicInfo senderInfo){
        this.messageType = MessageType;
        this.priority = priority;
        this.senderInfo = senderInfo;
    }

    public int getPriority() {
        return priority;
    }
    public MessageType getType() {
        return messageType;
    }
    public PlayerBasicInfo getSenderInfo(){
        return senderInfo;
    }
    @Override
    public int compareTo(BaseMessage other) {
        // Implement your comparison logic here
        // For example, compare messages based on content length
        return Integer.compare(this.priority, other.priority);
    }
}
