package message;

public class MessageWithDeadPlayer extends BaseMessage {

    private PlayerBasicInfo deadPlayer;
    public MessageWithDeadPlayer(message.MessageType type, int priority, PlayerBasicInfo senderInfo, PlayerBasicInfo deadPlayer){
        super(type, priority, senderInfo);
        this.deadPlayer = deadPlayer;
    }

    public PlayerBasicInfo getDeadPlayer() {
        return deadPlayer;
    }

}
