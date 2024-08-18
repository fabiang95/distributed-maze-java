package message;

import game.GameState;

public class MessageWithMessage extends BaseMessage{
    private final BaseMessage message;
    public MessageWithMessage(message.MessageType type, int priority, PlayerBasicInfo senderInfo, BaseMessage message){
        super(type, priority, senderInfo);
        this.message = message;
    }

    public BaseMessage getOriginalMessage() {

        return message;
    }
}
