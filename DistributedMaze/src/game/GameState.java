package game;

import message.PlayerBasicInfo;
import message.PlayerGameInfo;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.Serializable;

public class GameState implements Serializable{
    private BoardState board;
    private HashMap<String, PlayerGameInfo> activePlayers;


    private PlayerBasicInfo primaryServer;
    private PlayerBasicInfo secondaryServer;
    private int counter = 0;

    public final int N;
    public final int K;

    public final String gameStartTime;

    public GameState(int N, int K){
        this.N = N;
        this.K = K;
        System.out.println("making a new game state");
        board = new BoardState(N, K);
        activePlayers = new HashMap<>();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        gameStartTime = formatter.format(date);
    }
    public HashMap<String, PlayerGameInfo> getActivePlayers(){
        return activePlayers;
    }
    public PlayerBasicInfo getPrimaryServer(){
        return primaryServer;
    }
    public PlayerBasicInfo getSecondaryServer(){
        return secondaryServer;
    }

    public synchronized void updatePrimaryServer(PlayerBasicInfo p){
        primaryServer = p;
    }
    public synchronized void updateSecondaryServer(PlayerBasicInfo p){
        secondaryServer = p;
    }

    public boolean hasPlayerAt(Coordinate c){
        return board.hasPlayerAt(c);
    }
    public boolean hasTreasureAt(Coordinate c){
        return board.hasTreasureAt(c);
    }


    public synchronized ArrayList<PlayerBasicInfo> getPlayerList(){
        ArrayList<PlayerBasicInfo> playersList = new ArrayList<>();
        for (PlayerGameInfo p : activePlayers.values()){
            playersList.add(new PlayerBasicInfo(p.getName(), p.getIp(), p.getPort()));
        }
        return playersList;
    }
    public String getNameAt(Coordinate c){
        if (board.hasPlayerAt(c)){
            return board.getNameAt(c);
        }
        else {
            //TODO: placeholder for now, implement exceptions
            System.out.println("player was not found");
            return "XXX";
        }
    }
    public void incrementCounter(){
        counter ++;
    }
    public int getCounter(){
        return counter;
    }
    public Coordinate getPlayerPosition(String playerName){
        isSynced(playerName);
        return activePlayers.get(playerName).getLocation();
    }

    public synchronized void initializePlayer(String playerName, String playerIP, int playerPort){
        // TODO: only handles up to N players right now
        for (int x = N-1; x >= 0; x--){
            Coordinate startingPos = new Coordinate(x, N-1);
            if (!board.hasPlayerAt(startingPos)){
                PlayerGameInfo PlayerGameInfo = new PlayerGameInfo(playerName, playerIP, playerPort, startingPos, 0);
                activePlayers.put(playerName, PlayerGameInfo);
                board.initPlayer(playerName, startingPos);
                break;
            }
        }
    }

    public synchronized void removePlayer(String playerName){
        // checks that player is in activePlayers, then removes from activePlayers and board
        isSynced(playerName);

        Coordinate playerLoc = activePlayers.get(playerName).getLocation();
        board.removePlayerAt(playerLoc);
        activePlayers.remove(playerName);
        if (secondaryServer != null) {
            if (playerName.equals(secondaryServer.getName())){
                secondaryServer = null;
            }
        }
        if (primaryServer != null) {
            if (playerName.equals(primaryServer.getName())){
                primaryServer = null;
            }
        }

    }

    public synchronized void movePlayer(String playerName, Coordinate c){
        // allows arbitrary jumping anywhere, ensures player is currently on game board,
        // moves player to c if c is clear,
        // collects the treasure at c if there is treasure

        isSynced(playerName);

        if (board.hasPlayerAt(c)){
            return;
        }
        Coordinate currentPos = activePlayers.get(playerName).getLocation();
        board.movePlayer(currentPos, c);
        activePlayers.get(playerName).setLocation(c);

        if (board.hasTreasureAt(c)){
            activePlayers.get(playerName).incrementScore();
            board.collectTreasureAt(c);
        }

        isSynced(playerName);

    }

    private  synchronized void isSynced(String playerName){
        // checks internal representations in gameState is consistent
        assert (activePlayers.containsKey(playerName));
        Coordinate c = activePlayers.get(playerName).getLocation();
        assert (board.hasPlayerAt(c));
        assert (playerName.equals(board.getNameAt(c)));
    }




}
