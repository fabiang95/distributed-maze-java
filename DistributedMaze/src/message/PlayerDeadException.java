package message;

public class PlayerDeadException extends Exception{
    private final PlayerBasicInfo deadPlayerInfo;
    public PlayerDeadException(PlayerBasicInfo playerInfo){
        deadPlayerInfo = playerInfo;
    }
    public PlayerBasicInfo getDeadPlayer(){
        return deadPlayerInfo;
    }
}
