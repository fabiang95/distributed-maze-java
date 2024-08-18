package tracker;

import message.PlayerBasicInfo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

public class Tracker implements RmiTracker {
    private ArrayList<PlayerBasicInfo> playersList = new ArrayList<>();
    public final int N;
    public final int K;
    public final int portNumber;
    public Tracker(int portNumber, int N, int K){
        super();
        this.N = N;
        this.K = K;
        this.portNumber = portNumber;
    }
    public int getN(){
        return N;
    }
    public int getK(){
        return K;
    }
    public ArrayList<PlayerBasicInfo> registerPlayer(PlayerBasicInfo player) {
        System.out.println("tracker is registering " + player.toString() + " to rmi");
        playersList.add(player);
        return playersList;
    }
    public ArrayList<PlayerBasicInfo> deRegisterPlayer(PlayerBasicInfo player) {
        System.out.println("tracker is removing " + player.toString() + " from rmi");

        playersList.removeIf(p -> player.getName().equals(p.getName()));

        System.out.println("current player list: "+ playersList);
        return playersList;
    }

    public static void main(String args[]){
        try{
            String name = "Tracker";
            int port = Integer.parseInt(args[0]);
            int N = Integer.parseInt(args[1]);
            int K = Integer.parseInt(args[2]);
            RmiTracker tracker = new Tracker(port, N, K);
            LocateRegistry.createRegistry(port);
            RmiTracker stub = (RmiTracker) UnicastRemoteObject.exportObject(tracker, port);
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind(name, stub);
            System.out.println("Tracker bounded to rmi registry, running on port " + port);
        } catch (Exception e){
            System.err.println("error in binding tracker");
            e.printStackTrace();
        }

    }
}
