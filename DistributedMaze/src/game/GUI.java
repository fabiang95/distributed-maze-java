package game;
import message.PlayerGameInfo;

import javax.swing.JPanel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class GUI extends JPanel implements Runnable {
    final int tileSize = 20;
    final int scoreBoardSize = 6*tileSize;
    final int titleStripSize = 2*tileSize;
    final String displayName;
    GameState gameState;
    Thread gameThread;
    public GUI(GameState g, String displayName){
        this.gameState = g;
        this.displayName = displayName;
        this.setPreferredSize(new Dimension(scoreBoardSize + gameState.N*tileSize, titleStripSize + gameState.N*tileSize));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
    }

    public void startGameThread(){
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while(gameThread != null){
            repaint();
        }
    }
    public void changeGameState(GameState gameState){
        this.gameState = gameState;
        renderGameState();
    }
    private void renderGameState(){
        repaint();
    }
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        FontMetrics fontMetrics = g2d.getFontMetrics();

        g2d.setColor(Color.white);

        // title and time
        g2d.drawString(displayName + " POV  |  Game started @ " + gameState.gameStartTime, tileSize/2, titleStripSize/2 + fontMetrics.getHeight()/2);

        // paint the scoreboard
        HashMap<String, PlayerGameInfo> activePlayers = gameState.getActivePlayers();
        int startX = tileSize/4;
        int startY = fontMetrics.getHeight() + titleStripSize;
        int gap = (int) (fontMetrics.getHeight()*1.1); //vertical gap
        int space = 2*tileSize; // horizontal space

        String primaryName = "NA";
        if (gameState.getPrimaryServer() != null){
            primaryName = gameState.getPrimaryServer().getName();
        }
        String secondaryName = "NA";
        if (gameState.getSecondaryServer() != null){
            secondaryName = gameState.getSecondaryServer().getName();
        }

        g2d.drawString("primary = " + primaryName, startX, startY);
        startY = startY + gap;

        g2d.drawString("secondary = " + secondaryName, startX, startY);

        startY = startY + gap;
        g2d.drawLine(startX, startY, scoreBoardSize-startX, startY);
        startY = startY + gap;

        g2d.drawString("players & scores", startX, startY);

        startY = startY + gap;
        startX = tileSize/2;
        for (Map.Entry<String, PlayerGameInfo> player : activePlayers.entrySet()) {
            PlayerGameInfo playerGameInfo = player.getValue();
            g2d.drawString(playerGameInfo.getName(), startX, startY);
            g2d.drawString(String.valueOf(playerGameInfo.getScore()), startX+space, startY);
            startY = startY + gap;
        }

        // paint the maze
        g2d.drawLine(scoreBoardSize, titleStripSize, scoreBoardSize, this.getHeight());
        g2d.drawLine(0, titleStripSize, this.getWidth(), titleStripSize);
        for (int x = 0; x < gameState.N; x++){
            for (int y = 0; y < gameState.N; y++){
                Coordinate c = new Coordinate(x,y);

                String name = "";
                if (gameState.hasPlayerAt(c)){
                    name = gameState.getNameAt(c);
                }
                if (gameState.hasTreasureAt(c)){
                    name = "*";
                }
                if (!name.isEmpty()){
                    int width = fontMetrics.stringWidth(name);
                    int height = fontMetrics.getHeight();
                    g2d.drawString(name, scoreBoardSize+c.getX()*tileSize + tileSize/2 - width/2, titleStripSize + c.getY()*tileSize +tileSize/2 + height/2);
                }

            }
        }


        g2d.dispose();
    }
}
