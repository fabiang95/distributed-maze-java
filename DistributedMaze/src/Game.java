import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import game.Coordinate;
import game.GUI;
import game.GameState;
import message.*;
import tracker.RmiTracker;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;


public class Game {
    public Registry trackerRegistry;
    public final PlayerBasicInfo playerInfo;
    public final String playerName;
    public final String playerIp;
    public final int playerPort;
    public final String trackerIp;
    public final int trackerPort;
    public RmiTracker tracker;
    private ServerSocket listenerServer;
    private ArrayList<PlayerBasicInfo> playerList;
    private final PriorityBlockingQueue<BaseMessage> messageQueue;
    private final LinkedBlockingQueue<Integer> inputQueue;
    public boolean isPrimary = false;
    public boolean isSecondary = false;
    private int N;
    private int K;
    private GameState localGamestate;
    private GUI gameGUI;
    private synchronized void setPrimary() {
        // sets self as primary and updates local game state
        this.isPrimary = true;
        this.isSecondary = false;

        if (localGamestate == null){
            localGamestate = new GameState(N, K);
            localGamestate.initializePlayer(playerName, playerIp, playerPort);
        }
        localGamestate.updatePrimaryServer(playerInfo);
        localGamestate.updateSecondaryServer(null);
        localGamestate.incrementCounter();
        System.out.println(playerName + " became primary");
    }
    private synchronized void setSecondary(GameState gameState) {
        this.isPrimary = false;
        this.isSecondary = true;
        this.localGamestate = gameState;
        System.out.println(playerName + " became secondary");
    }
    private void checkServerConditions() {
        if (playerList.size() == 1) {
            setPrimary();
        } else if (playerList.size() == 2) {
            BaseMessage m = new BaseMessage(MessageType.CHECK_ALIVE, 4, playerInfo);
            for (PlayerBasicInfo player :playerList){
                if (!player.getName().equals(playerName)){
                    try {
                        sendMessage(player, m);
                    } catch (PlayerDeadException e) {
                        System.out.println("Game has ended");
                    }
                }
            }

        }
    }
    public Game(String trackerIp, int trackerPort, String name){

        messageQueue = new PriorityBlockingQueue<>(200);
        inputQueue = new LinkedBlockingQueue<>(200);
        this.trackerIp = trackerIp;
        this.trackerPort = trackerPort;
        this.playerName = name;
        this.playerIp = getPlayerIp();
        this.playerPort = startListener();

        playerInfo = new PlayerBasicInfo(name, playerIp, playerPort);

        bindToTracker();

        startNode();
        System.out.println("player " + playerName + " started game " + N + " " + K);
    }
    public String getPlayerIp(){
        return "localhost";
    }

    private synchronized int startListener(){
        int port;
        try {
            System.out.println("start listener");
            listenerServer = new ServerSocket(0); //starts on localhost, can change later, 0 assigns random available port
            System.out.println("Server started");

        } catch (IOException e){

            e.printStackTrace();
            System.out.println("failed to start listener");
        }
        port = listenerServer.getLocalPort();
        System.out.println(playerName + " established, listening on port "+ port);
        return port;
    }

    public void startNode(){
        Thread t1 = new Thread(new PlayerInput());
        t1.start();
        Thread t2 = new Thread(new MessageListener());
        t2.start();

        Thread t3 = new Thread(new MessageManager());
        t3.start();

        Thread t4 = new Thread(new ProcessInput());
        t4.start();

        Thread t5 = new Thread(new CheckAlive());
        t5.start();

        startGame();

        System.out.println("after start game");

    }

    private synchronized void nominateSecondary(PlayerBasicInfo nomineeInfo){
        assert isPrimary;
        localGamestate.updateSecondaryServer(nomineeInfo);
        localGamestate.incrementCounter();
        System.out.println("primary "+playerName + " nominated " + nomineeInfo.getName() + " as secondary");
        MessageWithGameState m = new MessageWithGameState(MessageType.NOMINATE_SECONDARY, 1, playerInfo, localGamestate);

        try {
            sendMessage(nomineeInfo, m);
        } catch (PlayerDeadException e){
            markDeadPlayer(e.getDeadPlayer());
        }

    }


    private void sendMessage(PlayerBasicInfo receiver, BaseMessage m) throws PlayerDeadException{
        Socket socket;
        // to disallow sending messages to your self
        if (!receiver.getName().equals(playerName)){
            try
            {
                socket = new Socket(receiver.getIp(), receiver.getPort());
                OutputStream outputStream = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                oos.writeObject(m);
                oos.close();
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("message from "+ playerName + " to " + receiver.getName() + " failed");
                throw new PlayerDeadException(receiver);
            }
        }

    }
    private class MessageManager implements Runnable {
        ExecutorService executor = Executors.newFixedThreadPool(5);

        public MessageManager() {
        }

        @Override
        public void run() {
            while (true) {
                BaseMessage message = null;
                try {
                    message = messageQueue.take();
                    Runnable MessageProcessor = new MessageProcessor(message);
                    executor.execute(MessageProcessor);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    private class MessageProcessor implements Runnable {

        private final BaseMessage message;

        public MessageProcessor(BaseMessage message) {
            this.message = message;
        }

        @Override
        public void run() {

            if (message.getType() == MessageType.UPDATE) {
                MessageWithGameState m = (MessageWithGameState) message;
                GameState g = m.getGameState();
                compareAndChangeGamestate(g);
                System.out.println("received update from primary");
            }
            if (message.getType() == MessageType.REQUEST_GAMESTATE) {
                // game state request from another player
                PlayerBasicInfo receiver = message.getSenderInfo();

                MessageWithGameState m = new MessageWithGameState(MessageType.INIT, 1, playerInfo, localGamestate);
                try {
                    sendMessage(receiver, m);
                    System.out.println(playerName + " sent gamestate to "+receiver.getName());
                } catch (PlayerDeadException e){
                    reportDeadPlayer(e.getDeadPlayer());
                }
            }
            if (message.getType() == MessageType.INIT) {
                MessageWithGameState m = (MessageWithGameState) message;
                PlayerBasicInfo primaryServer = m.getGameState().getPrimaryServer();
                BaseMessage messageToSend = new BaseMessage(MessageType.REQUEST_UPDATE, 2, playerInfo);
                try {
                    sendMessage(primaryServer, messageToSend);
                    System.out.println(playerName + " asked primary for gs");
                } catch (PlayerDeadException e){
                    reportPrimaryDead(messageToSend);
                }
            }
            if (message.getType() == MessageType.REQUEST_UPDATE) {
                // an update request received by primary from another player
                // first check if player exists, and initialize, otherwise send back most updated state
                assert isPrimary;
                PlayerBasicInfo player = message.getSenderInfo();
                if (!localGamestate.getActivePlayers().containsKey(player.getName())) {
                    // this player does not exist, initialize and increment gamestate counter
                    localGamestate.initializePlayer(player.getName(), player.getIp(), player.getPort());
                    playerList = localGamestate.getPlayerList();
                    localGamestate.incrementCounter();
                }
                MessageWithGameState updateMessage = new MessageWithGameState(MessageType.UPDATE, 2, playerInfo, localGamestate);
                try {
                    sendMessage(player, updateMessage);
                } catch (PlayerDeadException e){
                    markDeadPlayer(e.getDeadPlayer());
                }
                SyncWithSecondary();
            }
            if (message.getType() == MessageType.ATTEMPT_MOVE) {
                // received by primary when a player tries to move
                assert isPrimary;
                MessageWithMoveRequest moveMessage = (MessageWithMoveRequest) message;
                PlayerBasicInfo player = moveMessage.getSenderInfo();
                Direction direction = moveMessage.getDirection();
                movePlayer(direction, player.getName());

                MessageWithGameState updateMessage = new MessageWithGameState(MessageType.UPDATE, 4, playerInfo, localGamestate);
                try {
                    sendMessage(player, updateMessage);
                } catch (PlayerDeadException e){
                    markDeadPlayer(e.getDeadPlayer());
                }
                SyncWithSecondary();
            }

            if (message.getType() == MessageType.NOMINATE_SECONDARY) {
                // message sent from primary to promote this node to secondary
                assert !isPrimary;
                MessageWithGameState m = (MessageWithGameState) message;
                setSecondary(m.getGameState());
            }
            if (message.getType() == MessageType.SYNC_SECONDARY) {
                // receives a sync from primary, updates local gamestate to be the same as primary's
                assert isSecondary;
                MessageWithGameState m = (MessageWithGameState) message;
                localGamestate = m.getGameState();
                playerList = localGamestate.getPlayerList();
                gameGUI.changeGameState(localGamestate);
            }

            if (message.getType() == MessageType.UPDATE_DEAD) {
                assert isPrimary;
                MessageWithDeadPlayer updateDeadMessage = (MessageWithDeadPlayer) message;

                markDeadPlayer(updateDeadMessage.getDeadPlayer());
                MessageWithGameState updateMessage = new MessageWithGameState(MessageType.UPDATE, 1, playerInfo, localGamestate);

                // if this player was not the one who sent the message

                try {
                    sendMessage(updateDeadMessage.getSenderInfo(), updateMessage);
                } catch (PlayerDeadException e){
                    markDeadPlayer(e.getDeadPlayer());
                }

                SyncWithSecondary();
            }

            if (message.getType() == MessageType.PRIMARY_DEATH){
                assert isSecondary;
                MessageWithMessage m = (MessageWithMessage) message;
                promoteToPrimaryAndHandleMessage(m.getOriginalMessage());

            }
        }
    }

    private synchronized void promoteToPrimaryAndHandleMessage(BaseMessage message){
        assert isSecondary;
        setPrimary();
        messageQueue.add(message);
    }

    private void reportPrimaryDead(BaseMessage forwardedMessage){
        if (isSecondary){
            promoteToPrimaryAndHandleMessage(forwardedMessage);
        }
        if (localGamestate.getSecondaryServer() != null){
            try{
                MessageWithMessage messageToSecondary = new MessageWithMessage(MessageType.PRIMARY_DEATH, 1, playerInfo, forwardedMessage);
                sendMessage(localGamestate.getSecondaryServer(), messageToSecondary);
            } catch (PlayerDeadException e){
                throw new RuntimeException("Secondary died too, this should not happen");
            }
        }
    }

    private void SyncWithSecondary(){
        // message that primary sends to secondary to sync their gamestates
        assert isPrimary;

        if (localGamestate.getSecondaryServer() != null){
            PlayerBasicInfo secondaryServer = localGamestate.getSecondaryServer();
            MessageWithGameState syncMessage = new MessageWithGameState(MessageType.SYNC_SECONDARY, 1, playerInfo, localGamestate);
            try {
                sendMessage(secondaryServer, syncMessage);
            } catch (PlayerDeadException e){
                markDeadPlayer(e.getDeadPlayer());
            }
            System.out.println("primary "+ playerName + " performed sync with secondary "+ localGamestate.getSecondaryServer().getName());
        }

        if (localGamestate.getSecondaryServer() == null && playerList.size() > 1){
            nominateSecondary(getRandomPlayerInfo());
        }

    }

    private synchronized void markDeadPlayer(PlayerBasicInfo deadPlayerInfo){
        assert isPrimary;
        System.out.println(playerName + "'s markDeadPlayer method, dead player info: " + deadPlayerInfo);
        String deadPlayer = deadPlayerInfo.getName();

        if (localGamestate.getActivePlayers().containsKey(deadPlayer)){
            localGamestate.removePlayer(deadPlayer);
            localGamestate.incrementCounter();
            deregisterFromTracker(deadPlayerInfo); // which also updates the players list

            System.out.println(playerName + " marking dead player " + deadPlayer);

            if (localGamestate.getSecondaryServer() == null && playerList.size() > 1){
                nominateSecondary(getRandomPlayerInfo());
            }
        }
        SyncWithSecondary();
    }

    private void reportDeadPlayer(PlayerBasicInfo deadPlayerInfo){
        MessageWithDeadPlayer message = new MessageWithDeadPlayer(MessageType.UPDATE_DEAD, 1, playerInfo, deadPlayerInfo);
        if (isPrimary){
            System.out.println("primary marking dead player "+deadPlayerInfo.getName());
            markDeadPlayer(deadPlayerInfo);
        }
        else {
            if (deadPlayerInfo.getName().equals(localGamestate.getPrimaryServer().getName())) {
                reportPrimaryDead(message);
            } else {
                try {
                    sendMessage(localGamestate.getPrimaryServer(), message);
                } catch (PlayerDeadException e) {
                    reportPrimaryDead(message);
                }
            }
            // if player is not primary, will remove dead player locally on game state but not increment,
            // however removes this player locally so it does not continue to ping this player
            localGamestate.removePlayer(deadPlayerInfo.getName());
            playerList = localGamestate.getPlayerList();
        }
    }

    private class MessageListener implements Runnable {
        public MessageListener() {
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket listenerSocket = listenerServer.accept();

                    InputStream is = listenerSocket.getInputStream();
                    ObjectInputStream ois = new ObjectInputStream(is);

                    BaseMessage message = (BaseMessage) ois.readObject();
                    messageQueue.add(message);
                } catch (Exception e) {

                    e.printStackTrace();
                    System.out.println("failed to listen on listener socket");
                }
            }
        }


    }

    private synchronized void compareAndChangeGamestate(GameState g) {
        if (localGamestate != null) {
            if (g.getCounter() > localGamestate.getCounter()) {
                localGamestate = g;
            }
        } else {
            localGamestate = g;
        }
        playerList = localGamestate.getPlayerList();
        if (gameGUI != null) {
            gameGUI.changeGameState(localGamestate);
        }
    }

    private PlayerBasicInfo getRandomPlayerInfo() {
        assert (playerList.size() > 1);
        // Generating random selector
        ArrayList<Integer> shuffledIndices = new ArrayList<>(IntStream.range(0, playerList.size()).boxed().toList());
        Collections.shuffle(shuffledIndices);
        for (int index: shuffledIndices){
            PlayerBasicInfo selectedPlayer = playerList.get(index);
            if (!selectedPlayer.getName().equals(playerName)) {
                return selectedPlayer;
            }
        }
        return null;
    }

    private void getUpdate() {
        PlayerBasicInfo primaryServer = localGamestate.getPrimaryServer();
        BaseMessage messageToSend = new BaseMessage(MessageType.REQUEST_UPDATE, 4, playerInfo);
        try {
            sendMessage(primaryServer, messageToSend);
        }
        catch (PlayerDeadException e){
            MessageWithDeadPlayer messageDead = new MessageWithDeadPlayer(MessageType.UPDATE_DEAD, 1, playerInfo, e.getDeadPlayer());
            reportPrimaryDead(messageDead);
        }
    }

    private synchronized void movePlayer(Direction d, String playerName) {
        // move a player on the game state, can only be done by primary
        assert isPrimary;
        System.out.println("primary "+ this.playerName + " moved " + playerName + " " + d);
        Coordinate c = localGamestate.getPlayerPosition(playerName);
        Coordinate newPos = c;
        switch (d) {
            case WEST -> newPos = new Coordinate(Math.max(c.getX() - 1, 0), c.getY());
            case SOUTH -> newPos = new Coordinate(c.getX(), Math.min(c.getY() + 1, N - 1));
            case EAST -> newPos = new Coordinate(Math.min(c.getX() + 1, N - 1), c.getY());
            case NORTH -> newPos = new Coordinate(c.getX(), Math.max(c.getY() - 1, 0));
            default -> {
            }
        }
        localGamestate.movePlayer(playerName, newPos);
        localGamestate.incrementCounter();
        gameGUI.changeGameState(localGamestate);
    }

    private void tryToMove(Direction d) {
        System.out.println("player "+playerName + " tried to move " + d);
        if (isPrimary) {
            movePlayer(d, playerName);
            SyncWithSecondary();
        } else {
            MessageWithMoveRequest moveMessage = new MessageWithMoveRequest(MessageType.ATTEMPT_MOVE, 4, playerInfo, d);
            PlayerBasicInfo primary = localGamestate.getPrimaryServer();
            try{
                sendMessage(primary, moveMessage);
            } catch (PlayerDeadException e){
                MessageWithDeadPlayer messageDead = new MessageWithDeadPlayer(MessageType.UPDATE_DEAD, 1, playerInfo, e.getDeadPlayer());
                reportPrimaryDead(messageDead);
            }

        }
    }
    private boolean checkIfPrimary(PlayerBasicInfo player){
        if (localGamestate.getPrimaryServer() == null){
            return false;
        } else {
            return localGamestate.getPrimaryServer().getName().equals(player.getName());
        }

    }
    private boolean checkIfSecondary(PlayerBasicInfo player){
        if (localGamestate.getSecondaryServer() == null){
            return false;
        } else {
            return localGamestate.getSecondaryServer().getName().equals(player.getName());
        }

    }
    private class CheckAlive implements Runnable {
        @Override
        public void run() {
            while (true) {
                PlayerBasicInfo receiver = getRandomPlayerInfo();

                if (receiver != null && localGamestate != null){
                    BaseMessage m = new BaseMessage(MessageType.CHECK_ALIVE, 4, playerInfo);
                    try {
                        sendMessage(receiver, m);
                    } catch (PlayerDeadException e){
                        System.out.println(playerName + " detected that " + receiver.getName() + " is dead");
                        if (checkIfPrimary(receiver) && isSecondary){
                            setPrimary();
                            markDeadPlayer(receiver);
                        } else{
                            reportDeadPlayer(receiver);
                        }

                    }
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }


    private class ProcessInput implements Runnable {
        public ProcessInput(){

        }
        @Override
        public void run() {
            try {
                while (true) {
                    if (!inputQueue.isEmpty() && localGamestate != null) {
                        int playerMove = inputQueue.take();
                        switch (playerMove) {
                            case 0 -> getUpdate();
                            case 1 -> tryToMove(Direction.WEST);
                            case 2 -> tryToMove(Direction.SOUTH);
                            case 3 -> tryToMove(Direction.EAST);
                            case 4 -> tryToMove(Direction.NORTH);
                            default -> {
                            }
                        }
                    }

                }

            } catch (InterruptedException e){
                e.printStackTrace();
                System.out.println("Couldn't take from input queue");
            }

        }

    }
    private class PlayerInput implements Runnable {

        public PlayerInput() {
        }

        @Override
        public void run() {
            while (true) {
                Scanner keyboard = new Scanner(System.in);
                int playerMove = keyboard.nextInt();

                try {
                    inputQueue.put(playerMove);
                } catch (InterruptedException e){
                    e.printStackTrace();
                    System.out.println("couldn't add input into player input queue");
                }

            }

        }
    }

    public void bindToTracker() {
        try {
            trackerRegistry = LocateRegistry.getRegistry(trackerIp, trackerPort);
            RmiTracker tracker = (RmiTracker) trackerRegistry.lookup("Tracker");

            System.out.println(playerName + " joined");
            PlayerBasicInfo nodeInfo = new PlayerBasicInfo(playerName, playerIp, playerPort);

            playerList = tracker.registerPlayer(nodeInfo);
            this.N = tracker.getN();
            this.K = tracker.getK();
//            System.out.println(playerList);

        } catch (Exception e) {

            e.printStackTrace();
            System.err.println("locating tracker exception: ");
        }
    }


    private void deregisterFromTracker(PlayerBasicInfo deadPlayer){
        assert isPrimary;
        try {
            RmiTracker tracker = (RmiTracker) trackerRegistry.lookup("Tracker");
            System.out.println(deadPlayer + " left");
            playerList = tracker.deRegisterPlayer(new PlayerBasicInfo(deadPlayer.getName(), deadPlayer.getIp(), deadPlayer.getPort()));
        } catch (Exception e) {

            e.printStackTrace();
            System.err.println("locating tracker exception: ");
        }
    }


    public static void main(String[] args) {
        // run in terminal: java Node [IP-address] [port-number] [player-id]
        // args[0] is Tracker's IP
        // args[1] is Tracker's port
        // args[2] is player name
        String trackerIp = args[0];
        int trackerPort = Integer.parseInt(args[1]);
        String playerName = args[2];

        System.out.println("hello node");
        Game n = new Game(trackerIp, trackerPort, playerName);

    }

    private void initializePlayer() {
        // check the playersList, if only player, become the primary server
        // else, find random player and contact
        if (playerList.size() == 1) {
            setPrimary();

        } else {
            requestInitialGameState();
            System.out.println("player " + playerInfo.getName()  + " is requesting game state");
        }
    }

    private void requestInitialGameState() {
        //TODO: right now potential for inifinte loop
        // contact a random node to get game state
        // then contact primary for update
        PlayerBasicInfo receiver = getRandomPlayerInfo();
        BaseMessage m = new BaseMessage(MessageType.REQUEST_GAMESTATE, 2, playerInfo);
        try {
            sendMessage(receiver, m);
        } catch (PlayerDeadException e){
            reportDeadPlayer(e.getDeadPlayer());
            initializePlayer();
        }
    }

    private void startGame() {


//        System.out.println("Player " + playerName + " is starting a game");

        initializePlayer();


        JFrame window = new JFrame();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("Distributed Maze");

        gameGUI = new GUI(localGamestate, playerName);
        window.add(gameGUI);

        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        gameGUI.startGameThread();

    }
}


