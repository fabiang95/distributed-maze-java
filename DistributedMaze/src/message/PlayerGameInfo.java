package message;

import game.Coordinate;

public class PlayerGameInfo extends PlayerBasicInfo {
    private Coordinate location;
    private int score;

    public PlayerGameInfo(String name, String ip, int port, Coordinate location, int score){
        super(name, ip, port);
        this.location = location;
        this.score = score;
    }
    public Coordinate getLocation(){
        return location;
    }
    public void setLocation(Coordinate c){
        this.location = c;
    }
    public void incrementScore(){
        score ++;
    }
    public int getScore(){
        return score;
    }

    @Override
    public String toString() {
        return getName() + "@" + getIp() + ":" + getPort() + "; score=" + getScore() + " (" + getLocation() + ")";
    }
}
