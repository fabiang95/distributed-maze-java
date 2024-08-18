package game;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class BoardState implements Serializable {
    // assumes that GameState will handle all the checking, so any move dictated for BoardState is correct
    public final int N;
    public final int K;
    private HashMap<Coordinate, String> playersMap;
    private HashSet<Coordinate> treasureLocations;

    public BoardState(int N, int K){
        this.N = N;
        this.K = K;
        playersMap = new HashMap<>();
        treasureLocations = new HashSet<>();
        generateTreasures(K);
    }

    private void generateTreasures(int K){
        // randomly generates K treasures on the map, ensuring the square is not occupied by player or treasure
        for (int t = 0; t < K; t++){
            Coordinate c;
            do {
                Random random = new Random();
                int x = random.nextInt(N);
                int y = random.nextInt(N);
                c = new Coordinate(x,y);
            }
            while (
                    treasureLocations.contains(c) || playersMap.containsKey(c) || c.getY() == N-1
            );
            treasureLocations.add(c);
        }
    }


    public boolean hasPlayerAt(Coordinate c){
        return playersMap.containsKey(c);
    }
    public String getNameAt(Coordinate c){
        return playersMap.get(c);
    }
    public boolean hasTreasureAt(Coordinate c){
        return treasureLocations.contains(c);
    }
    public void movePlayer(Coordinate oldPos, Coordinate newPos){
        String playerName = playersMap.get(oldPos);
        playersMap.remove(oldPos);
        playersMap.put(newPos, playerName);
    }
    public void initPlayer(String playerName, Coordinate c){
        playersMap.put(c, playerName);
    }

    public void removePlayerAt(Coordinate c){
        playersMap.remove(c);
    }
    public void collectTreasureAt(Coordinate c){
        // auto re-gens the treasure
        treasureLocations.remove(c);
        generateTreasures(1);
    }

}
