package message;

public class MessageWithMoveRequest extends BaseMessage{
    private Direction direction;
    public MessageWithMoveRequest(message.MessageType type, int priority, PlayerBasicInfo senderInfo, Direction direction){
        super(type, priority, senderInfo);
        this.direction = direction;
    }

    public Direction getDirection() {
        return direction;
    }
}
