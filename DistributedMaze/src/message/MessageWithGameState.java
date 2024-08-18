package message;

import game.GameState;

public class MessageWithGameState extends BaseMessage {
    private final GameState gameState;
    public MessageWithGameState(message.MessageType type, int priority, PlayerBasicInfo senderInfo, GameState gameState){
        super(type, priority, senderInfo);
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }
}
