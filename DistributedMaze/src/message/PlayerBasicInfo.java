package message;

import java.io.ObjectInputStream;
import java.io.Serializable;

public class PlayerBasicInfo implements Serializable {
    private final String name;
    private final String ip;
    private final int port;


    public PlayerBasicInfo(String name, String ip, int port){
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public String getName(){
        return name;
    }
    public String getIp(){
        return ip;
    }
    public int getPort(){
        return port;
    }


    @Override
    public String toString() {
        return name + "@" + ip + ":" + port;
    }

}
