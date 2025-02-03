import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import static java.lang.String.valueOf;

class Game {
    private static void initWindow () {
        JFrame window = new JFrame("Game");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // stop the app when we close the window

        GameConfig config = new GameConfig();
        Board board = new Board(config);
        window.add (board);
        window.addKeyListener(board); // pass keyboard inputs to the jpanel

        window.pack(); //fits the window to the components
        window.setLocationRelativeTo(null); // opens window in the center of the screen
        window.setResizable(false); // not allow the user to resize the window
        window.setVisible(true);
    }
    public static void main (String[] arg) {
        initWindow();
    }
}


class GameConfig {
    public final int TILE_WIDTH = 60, TILE_HEIGHT = 30, ROWS = 15, COLUMNS = 20, SIDE_SIZE = 130;
    public final int WIDTH_SCREEN = TILE_WIDTH*COLUMNS + 2 * SIDE_SIZE, HEIGHT_SCREEN = TILE_HEIGHT*ROWS + 4 * SIDE_SIZE;
    public final int DELAY = 25;
}


class Board extends JPanel implements ActionListener, KeyListener {
    private final Timer timer;
    private final GameConfig config;
    private final Player player;
    private final Block[][] blocksMatrix;
    private final Ball ball;
    private final ArrayList <Powerup> powerups; // list of the powerups currently active on screen
    private Powerup activePowerup = null;
    GameState state;
    Upgrade upgrade;

    public enum GameState {
        MENU,
        PLAYING,
        PAUSE,
        UPGRADE,
        GAME_OVER
    }

    public Board (GameConfig c) {
        this.config = c;

        setPreferredSize(new Dimension(config.WIDTH_SCREEN, config.HEIGHT_SCREEN));
        setBackground(Color.black);

        this.state = GameState.PLAYING;
        this.player = new Player(config);
        this.blocksMatrix = Block.createAllBlocks(config);
        this.ball = new Ball(config);

        this.powerups = new ArrayList<>();
        this.upgrade = new Upgrade();
        this.timer = new Timer(config.DELAY, this); // needs to have a listener
        this.timer.start();
    }

    @Override
    protected void paintComponent (Graphics g) {
        super.paintComponent(g);
        switch (state) {
            case GameState.PLAYING:
                drawPlaying(g);
                break;
            case PAUSE:
                drawPlaying(g);
                drawPause(g);
                break;
            case UPGRADE:
                drawPlaying(g);
                upgrade.draw(g);
                break;
            case GameState.GAME_OVER:
                drawGameOver(g);
                break;
            default:
                break;
        }
    }

    private void drawPlaying (Graphics g) {
        drawBackground(g);
        drawScore(g);
        player.draw(g);
        for (Block[] array : blocksMatrix) for (Block block : array) if (block != null) block.draw(g, config);
        ball.draw(g);
        if (! powerups.isEmpty()) for (Powerup pw : powerups) pw.draw(g);
    }

    private void drawTransparentSquare (Graphics g, int x, int y, int width, int height) {
        Color blackTransparent = new Color(25,25,25, 200);

        g.setColor(blackTransparent);
        g.fillRect(x,y, width, height);
    }

    private void drawPause (Graphics g) {
        int width = getWidth(), height = getHeight();
        int pauseWidth = width/2, pauseHeight = height/2;

        drawTransparentSquare (g, width /4,height/4, pauseWidth, pauseHeight);

        String text1 = "Paused";
        String text2 = "Press 'P' to continue playing";
        String text3 = "Press 'E' to leave the game";

        g.setColor(Color.white);
        g.drawString(text1, pauseWidth/2 + width/4 - g.getFontMetrics().stringWidth(text1) / 2, pauseHeight/4 + height/4);
        g.drawString(text2, pauseWidth/2 + width/4 - g.getFontMetrics().stringWidth(text2) / 2, pauseHeight*3/5 + height/4);
        g.drawString(text3, pauseWidth/2 + width/4 - g.getFontMetrics().stringWidth(text3) / 2, pauseHeight*4/5 + height/4);
    }

    private void drawBackground (Graphics g) {
        g.setColor(Color.darkGray);
        for (int row = 0; row <= config.ROWS; row++) {
            for (int col = 0; col <= config.COLUMNS; col++) {
                int x = config.SIDE_SIZE + col * config.TILE_WIDTH;
                int y = config.SIDE_SIZE + row * config.TILE_HEIGHT;
                int mostLeft = x-4, mostTop = y-4, mostRight = x+4, mostBottom = y+4;
                if (col == 0) mostLeft = x;
                if (col == config.COLUMNS) mostRight = x;
                if (row == 0) mostTop = y;
                if (row == config.ROWS) mostBottom = y;

                g.drawLine(mostLeft, y, mostRight, y);
                g.drawLine(x, mostTop, x, mostBottom);
            }
        }
        g.drawRect(config.SIDE_SIZE, config.SIDE_SIZE, config.COLUMNS*config.TILE_WIDTH, config.ROWS*config.TILE_HEIGHT);
    }

    private void drawScore (Graphics g) {
        int scoreNum = 0;
        scoreNum = ball.getScore();
        String score = "Score: " + scoreNum;
        String lives = "Lives: " + player.getLives();
        //String highscore = "Highscore: 0"; //read file

        String cooldownString;
        if (ball.areThereTemporaryBalls()) {
            int cooldown = (int) ((ball.temporaryCooldownEndTime - System.currentTimeMillis()) / 1000 + 1);
            if (cooldown < 0) cooldown = 0;
            cooldownString = "Cooldown: " + cooldown;
        }
        else cooldownString = "Cooldown: --";

        g.setColor(Color.white);
        g.drawString(lives, config.WIDTH_SCREEN/4 - g.getFontMetrics().stringWidth(lives) / 2, config.SIDE_SIZE/2);
        g.drawString(score, config.WIDTH_SCREEN*2/4 - g.getFontMetrics().stringWidth(score) / 2, config.SIDE_SIZE/2);
        g.drawString(cooldownString, config.WIDTH_SCREEN*3/4 - g.getFontMetrics().stringWidth(cooldownString) / 2, config.SIDE_SIZE/2);
    }

    @Override
    public void keyTyped(KeyEvent e){}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (state) {
            case GameState.PLAYING:
                if (e.getKeyCode() == KeyEvent.VK_P) pause();
                else {
                    player.keyPressed(e);
                    ball.keyPressed(e);
                }
                break;
            case GameState.UPGRADE:
                upgrade.chooseType(e);
                break;
            case GameState.PAUSE:
                if (e.getKeyCode() == KeyEvent.VK_P) resume();
                else if (e.getKeyCode() == KeyEvent.VK_E) triggerGameOver();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        player.keyReleased(e);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == GameState.PLAYING) {

            player.tick(config);
            if (!powerups.isEmpty()) Powerup.tick(config, powerups, activePowerup, player);
            if (activePowerup != null) {
                
            }
            int oldBallScore = ball.getScore();

            for (Block[] array : blocksMatrix) for (Block block : array) if (block != null) block.tick(blocksMatrix);
            ball.tick(config, blocksMatrix, player, powerups);

            if (ball.getClass() == Ball.class) {
                if (ball.getSpeed() <= 0 && ball.active) reset(config);
            }

            int newBallScore = ball.getScore();
            // upgrade conditions
            if (oldBallScore != newBallScore && oldBallScore % (1000 * (upgrade.numberOfUpgrades * 2 + 1)) > newBallScore % (1000 * (upgrade.numberOfUpgrades * 2 + 1))) upgrade.getUpgrade();
            if (player.getLives() == 0) triggerGameOver();
        }
        repaint ();
    }

    private void triggerGameOver() {
        state = GameState.GAME_OVER;
        repaint();

        Timer timer = new Timer(2000, _ -> System.exit(0));
        timer.setRepeats(false); // Ensure the timer only runs once
        timer.start();
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0,0, getWidth(), getHeight());

        String text = "Game over!";
        g.setColor(Color.WHITE);
        g.drawString(text, getWidth() / 2 - g.getFontMetrics().stringWidth(text) / 2, getHeight() / 2);
    }

    private void pause (){
        state = GameState.PAUSE;
        timer.stop();
        repaint();
    }

    private void resume () {
        state = GameState.PLAYING;
        timer.start();
        repaint();
    }

    private void reset (GameConfig config) {
        ball.initialState (config);
        player.initialState(config);
        player.loseLive();
    }

    public class Upgrade {
        public upgradeType type, choice1, choice2;
        private boolean choosing = false;
        private int numberOfUpgrades = 0;
        private long lastKeyPressTime = 0;

        public enum upgradeType {
            widenPlayer,
            enlargeBall,
            quickerPlayer,
            explosiveBall,
            moreDamage
        }

        public Upgrade () {
            randomChoices();
        }


        public void getUpgrade () {
            state = GameState.UPGRADE;
            timer.stop();
            repaint();
        }

        private void randomChoices () {
            Random ran = new Random();
            int choice1 = ran.nextInt(upgradeType.values().length);
            int choice2;
            this.choice1 = upgradeType.values()[choice1];
            do {
                choice2 = ran.nextInt(upgradeType.values().length); // pick 2 different choices at random
            } while (choice1 == choice2);
            this.choice2 = upgradeType.values()[choice2];
        }

        public void draw(Graphics g) { // drawing the menu
            int width = getWidth(), height = getHeight();
            int pauseWidth = width/2, pauseHeight = height/2;
            int pauseX = width/4, pauseY = height/4;
            drawTransparentSquare (g, pauseX,pauseY, pauseWidth, pauseHeight);

            int squareWidth = pauseWidth/3, squareHeight = pauseHeight/5, squareY = pauseHeight/2 + pauseY;
            int square1X = pauseX + 10, square2X = pauseWidth + pauseX - squareWidth - 10;
            g.setColor(Color.BLACK);
            g.fillRect(square1X, squareY, squareWidth, squareHeight);
            g.fillRect(square2X, squareY, squareWidth, squareHeight);

            g.setColor(Color.WHITE);
            if (choosing) g.drawRect(square2X, squareY, squareWidth, squareHeight);
            else g.drawRect(square1X, squareY, squareWidth, squareHeight);

            String choice1 = upgrade.choice1.toString();
            String choice2 = upgrade.choice2.toString();
            String text = "Choose one upgrade";
            g.drawString(text, pauseWidth / 2 - g.getFontMetrics().stringWidth(text) / 2 + pauseX, pauseHeight / 5 + pauseY);
            g.drawString(choice1, square1X + squareWidth/2 - g.getFontMetrics().stringWidth(choice1) / 2, squareY + squareHeight/2);
            g.drawString(choice2, square2X + squareWidth/2 - g.getFontMetrics().stringWidth(choice1) / 2, squareY + squareHeight/2);
        }

        public void chooseType (KeyEvent e) { // logic to choose the menu (may re-use)
            int DELAY = 200;
            long timePressed = System.currentTimeMillis();
            if (timePressed - lastKeyPressTime <= DELAY) return; // delay between presses
            lastKeyPressTime = timePressed;

            int key = e.getKeyCode();
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                choosing = !choosing;
            }
            else if (key == KeyEvent.VK_ENTER) {
                type = choosing ? choice2 : choice1;
                choosing = false;
                numberOfUpgrades++;
                randomChoices();
                gottenUpgrade();
            }
            repaint();
        }

        private void gottenUpgrade () {
            switch (upgrade.type) {
                case enlargeBall -> ball.enlargeBall();
                case widenPlayer -> player.widenPlayer();
                case quickerPlayer -> player.fastenPlayer();
                case moreDamage -> ball.moreDamage();
                case explosiveBall -> explosiveBallUpgrade();
            }
            state = GameState.PLAYING;
            timer.start();
            repaint();
        }

        private void explosiveBallUpgrade () {
            TemporaryBall newBall = new TemporaryBall(config);
            ball.addTempBall (newBall);
        }
    }
}

//--------------------------------------------------------------------------------------------------------

class Block {
    private int lives, col, row, speed; // col and row start on 0
    private Color color;
    private BlockType type;

    public enum BlockType {
        AVERAGE,
        STICKY,
        RESISTANT
    }

    public static Block[][] createAllBlocks (GameConfig config) {
        // fill the entire board with blocks
        Block[][] array = new Block[config.COLUMNS][config.ROWS];
        for (int x = 0; x < config.COLUMNS; x++) {
            for (int y = 0; y < config.ROWS; y++) {
                BlockType type;
                if (y < config.COLUMNS/4) type = BlockType.RESISTANT;
                else if (y < config.COLUMNS/2) type = BlockType.STICKY;
                else type = BlockType.AVERAGE;

                Random rand = new Random();
                int chance = rand.nextInt(4), choice = rand.nextInt(BlockType.values().length); // 1/5 chance of any block being a random type

                if (chance == 0) type = BlockType.values()[choice];

                Point p = new Point(x,y);
                Block b = createBlock (type,p);
                array[x][y] = b;
            }
        }
        return array;
    }


    public static Block createBlock (BlockType type, Point p) {
        Block block = new Block();
        switch (type) {
            case STICKY:
                block.lives = 1;
                block.speed = -2;
                block.color = new Color(50,205,50);
                break;
            case AVERAGE:
                block.lives = 1;
                block.speed = 1;
                block.color = new Color(255,223,0);
                break;
            case RESISTANT:
                block.lives = 2;
                block.speed = 0;
                block.color = new Color(200,42,42);
                break;
        }
        block.col = p.x;
        block.row = p.y;
        block.type = type;
        return block;
    }

    public int loseLife (Block block, int damage) {
        block.lives -= damage;
        Color oldColor = block.color;
        block.color = oldColor.brighter();
        if (block.lives <= 0) { // destroyed the block
            return ((block.type.ordinal() + 1) * 30); // points equal to the order of the enum, average = 30, sticky = 60, resistant = 90
        }
        else return 0;
    }

    public int getSpeed () {
        return this.speed;
    }

    public void draw (Graphics g, GameConfig config) {
        g.setColor (color);
        g.fillRect (col*config.TILE_WIDTH + config.SIDE_SIZE, row*config.TILE_HEIGHT + config.SIDE_SIZE, config.TILE_WIDTH, config.TILE_HEIGHT);
        g.setColor(Color.white);
        g.drawRect(col*config.TILE_WIDTH + config.SIDE_SIZE, row*config.TILE_HEIGHT + config.SIDE_SIZE, config.TILE_WIDTH, config.TILE_HEIGHT);
    }

    public void tick (Block [][] matrix) {
        if (lives <= 0) matrix[col][row] = null;
    }
}


//--------------------------------------------------------------------

class Player {
    private int lives = 3, posX, posY, vX, speed = 15;
    private int width = 70;
    private final int height = 10;

    private boolean left, right;

    public Player (GameConfig config) {
       initialState(config);
    }

    public void initialState (GameConfig config) {
        posX = config.WIDTH_SCREEN / 2;
        posY = config.HEIGHT_SCREEN - config.SIDE_SIZE / 2;

    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect (posX - width/2, posY - height/2, width, height);
        g.setColor(Color.WHITE);
        g.drawRect( posX - width/2, posY - height/2, width, height);
    }

    public void widenPlayer () { width += 10; }
    public void fastenPlayer () {speed += 5; }

    public int getWidth () {
        return width;
    }
    public Point getPosition (){
        return new Point(posX, posY);
    }
    public int getHeight () { return height; }
    public int getLives() {
        return lives;
    }

    public void loseLive() {
        lives --;
    }
    private void updateVel () {
        vX = 0;

        if (left) vX = -speed;
        if (right) vX = speed;
    }

    public void keyPressed (KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) {
                left = true;
        }
        if (key == KeyEvent.VK_RIGHT) {
                right = true;
        }
        updateVel();
    }

    public void keyReleased (KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_LEFT) {
            left = false;
        }
        if (key == KeyEvent.VK_RIGHT) {
            right = false;
        }
        updateVel();
    }

    public void tick (GameConfig config) {
        int leftLimit = config.SIDE_SIZE;
        int rightLimit = config.SIDE_SIZE + config.COLUMNS*config.TILE_WIDTH;

        if (posX < leftLimit) posX = leftLimit;
        else if (posX > rightLimit) posX = rightLimit;

        posX += vX;
    }
}


// --------------------------------------------------------------------------------


class Ball {
    protected int posX, posY, speed = 0, damage = 1, score = 0, angle = 0, size = 15, temporaryUsedTime = 0; // angle range is 0-360
    protected final int BASE_SPEED = 15, TEMPORARY_MAX_COOLDOWN = 5000, TEMPORARY_MAX_USED_TIME = 3000;
    protected long temporaryCooldownEndTime = -1;
    protected boolean active = false;
    private final ArrayList <TemporaryBall> temporaryBallList;
    static ArrayList<FloatingPoints> floatingPoints = new ArrayList<>();

    public Ball (GameConfig config) {
        this.temporaryBallList = new ArrayList<TemporaryBall>();
        initialState(config);
    }

    public boolean areThereTemporaryBalls () { return ! temporaryBallList.isEmpty(); }

    public void initialState (GameConfig config) {
        posX = config.WIDTH_SCREEN/2;
        posY = config.HEIGHT_SCREEN - config.SIDE_SIZE/2 - size - 10;
        speed = 0;
        active = false;
    }

    public void draw (Graphics g) {
        Color color = Utils.rainbowColor(100);
        g.setColor(color);
        g.fillOval(posX - size/2, posY - size/2, size, size);

        if (!floatingPoints.isEmpty()) for (FloatingPoints fp : floatingPoints) fp.draw(g);
        if (!temporaryBallList.isEmpty()) for (TemporaryBall temp : temporaryBallList) temp.draw(g);
    }

    public void addTempBall (TemporaryBall ball) {
        temporaryBallList.add (ball);
    }

    public int getScore() { return score;}

    public int getSpeed () { return speed; }

    public void enlargeBall (){ size += 5; }
    public void moreDamage () { damage ++; }

    private void updatePosition () {
        double radians = Math.toRadians(angle);
        posX += (int) (Math.cos(radians) * speed);
        posY -= (int) (Math.sin(radians) * speed); // minus cause y grows downwards (screen cords)
    }

    private void touchingPlayer(Player p) {
        if (Utils.touchingPlayer(this.posX, this.posY, this.size, p)) {
            angle = 360 - angle;
            speed += 3; // increase speed on hit
            updatePosition();
        }
    }

    private void touchingBlock (GameConfig config, Block [][] blocks, ArrayList <Powerup> powerups) {
        int col = (posX - config.SIDE_SIZE) / config.TILE_WIDTH;
        int row = (posY - config.SIDE_SIZE) / config.TILE_HEIGHT;
        // see if there's a block in the current position
        if (row >= config.ROWS || row < 0 || col < 0 || col >= config.COLUMNS) return;
        Block block = blocks[col][row];
        if (block == null) return;

        // Block boundaries
        int blockLeft = col * config.TILE_WIDTH + config.SIDE_SIZE;
        int blockRight = blockLeft + config.TILE_WIDTH;
        int blockTop = row * config.TILE_HEIGHT + config.SIDE_SIZE;
        int blockBottom = blockTop + config.TILE_HEIGHT;

        // Ball boundaries
        int ballLeft = posX - size / 2;
        int ballRight = posX + size / 2;
        int ballTop = posY - size / 2;
        int ballBottom = posY + size / 2;

        // Determine collision direction
        boolean hitFromLeft = ballRight > blockLeft && ballLeft < blockLeft && posX < blockLeft;
        boolean hitFromRight = ballLeft < blockRight && ballRight > blockRight && posX > blockRight;
        boolean hitFromTop = ballBottom > blockTop && ballTop < blockTop && posY < blockTop;
        boolean hitFromBottom = ballTop < blockBottom && ballBottom > blockBottom && posY > blockBottom;

        if ((hitFromLeft || hitFromRight) && (hitFromTop || hitFromBottom)) {
            angle = 180 + angle; // Diagonal hit
        } else if (hitFromLeft || hitFromRight) {
            angle = 180 - angle; // Horizontal hit
        } else {
            angle = 360 - angle; // Vertical hit
        }

        angle = (angle+360) % 360; //normalize angle

        this.speed += block.getSpeed();

        int points = block.loseLife(block, damage); // if destroyed a block, get points

        if (points != 0) { // if a block was destroyed,
            Combo.comboCounter++;
            Combo.comboTimer = Combo.RESET_TIME; // reset counter

            int comboBonus = Combo.comboCounter > 1 ? (Combo.comboCounter - 1) * 5 : 0; // Extra points per additional block
            this.score += points + comboBonus;
            floatingPoints.add(new FloatingPoints(posX, posY, points + comboBonus));

            Random ran = new Random();
            if (ran.nextInt(9) == 0) Powerup.spawnPowerup(powerups, config, posX, posY); // 1/10 chance of dropping a powerup
        }
    }


    public void tick (GameConfig config, Block[][] blocks, Player p, ArrayList <Powerup> powerups) {
        tickBall(config, blocks, p, powerups);

        if (!temporaryBallList.isEmpty()) for (TemporaryBall temp : temporaryBallList) temp.tickBall(config, blocks, p, powerups);
        if (!floatingPoints.isEmpty()) FloatingPoints.tick(Ball.floatingPoints, config);
        Combo.tick(config);
    }

    protected void printBall () { // for debug
        System.out.printf("X: %d, Y: %d, Speed: %d, Size: %d, Angle: %d\n", posX, posY, speed,size, angle);
    }

    protected void tickBall(GameConfig config, Block[][] blocks, Player p, ArrayList <Powerup> powerups) {
        updatePosition();
        if (posY < config.HEIGHT_SCREEN - 3*config.SIDE_SIZE) touchingBlock(config, blocks, powerups); //inside the area where blocks are
        else touchingPlayer (p);

        touchingBorder(config);

        final int cappedSpeed = 30;
        if (speed > cappedSpeed) speed = cappedSpeed;

        // extra ball management
        if (temporaryUsedTime > 0) temporaryUsedTime -= 1000 / config.DELAY;
        else if (temporaryUsedTime > -1000){
            temporaryCooldownEndTime = System.currentTimeMillis() + TEMPORARY_MAX_COOLDOWN;
            temporaryUsedTime = -1000;
            for (TemporaryBall temp : temporaryBallList) temp.setInactive();
        }
    }

    private void touchingBorder (GameConfig config) {
        final int LEFT_LIMIT = config.SIDE_SIZE;
        final int RIGHT_LIMIT = config.WIDTH_SCREEN - config.SIDE_SIZE;
        final int TOP_LIMIT = config.SIDE_SIZE;
        final int BOTTOM_LIMIT = config.HEIGHT_SCREEN;

        boolean bounced = false;

        if (posX <= LEFT_LIMIT || posX >= RIGHT_LIMIT) {
            posX = Math.max(LEFT_LIMIT, Math.min(posX, RIGHT_LIMIT));
            angle = 180 - angle;
            bounced = true;
        }
        if (posY <= TOP_LIMIT || posY >= BOTTOM_LIMIT) {
            posY = Math.max(TOP_LIMIT, Math.min(posY, BOTTOM_LIMIT));
            angle = 360 - angle;
            bounced = true;
        }

        if (bounced) {
            angle = (angle + 360) % 360; // normalize angle
            updatePosition(); // move again after bouncing to prevent sticking
        }
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (!active) {
            if (key == KeyEvent.VK_LEFT) angle = 135;
            else if (key == KeyEvent.VK_RIGHT) angle = 45;
            else return;

            active = true;
            speed = BASE_SPEED;
            updatePosition();
        }
        else if (key == KeyEvent.VK_SPACE) keyPressedTemporaryList(e);
    }

private void keyPressedTemporaryList (KeyEvent e) {
    long currentTime = System.currentTimeMillis();
    if (!temporaryBallList.isEmpty() && currentTime >= temporaryCooldownEndTime) {
        temporaryCooldownEndTime = currentTime + TEMPORARY_MAX_COOLDOWN; // start global cooldown

        int activeBalls = temporaryBallList.size();
        int i = 0;
        for (TemporaryBall temp : temporaryBallList) {
            int angle = this.angle + 360 / activeBalls * i;
            angle = (angle + 360) % 360;

            temp.angle = angle;
            temp.speed = BASE_SPEED - 5;
            temp.posX = this.posX;
            temp.posY = this.posY;
            temp.active = true;
            i++;
            }
        temporaryUsedTime = TEMPORARY_MAX_USED_TIME;
    }
}

    public static class FloatingPoints {
        private int posY;
        final private int posX, points;
        final static int MAX_DUR = 1000; // milliseconds
        private int duration; // time remaining

        public FloatingPoints (int x, int y, int points) {
            this.posX = x;
            this.posY = y;
            this.points = points;
            this.duration = MAX_DUR;
        }

        private boolean individualTick(GameConfig config) {
            duration -= config.DELAY; // Reduce remaining time
            posY -= 1; // Optional: Make the points float upward

            return duration <= 0;
        }

        public static void tick(ArrayList<FloatingPoints> array, GameConfig config) {
            array.removeIf(points -> points.individualTick(config));
        }

        public void draw (Graphics g) {
            Color color = Utils.rainbowColor(MAX_DUR / 7);
            g.setColor(color);
            g.drawString(valueOf(points), posX, posY);
        }

    }

    static class Combo {
        static int comboCounter = 0; // number of blocks destroyed in the combo
        static int comboTimer = 0; // time remaining to maintain the combo
        private static final int RESET_TIME = 1000; // milliseconds

        public Combo () {
            comboCounter = 0;
            comboTimer = 0;
        }

        public static void tick (GameConfig config) {
            if (comboTimer > 0) comboTimer -= config.DELAY;
            else {
                comboTimer = 0;
                comboCounter = 0;
            }
        }
    }
}


class TemporaryBall extends Ball {
    public TemporaryBall(GameConfig config) {
        super(config);
    }

    protected void setInactive () {
        active = false;
        speed = 0;
    }
    @Override
    protected void tickBall(GameConfig config, Block[][] blocks, Player p, ArrayList <Powerup> powerups) {
        if (!active) return;

        super.tickBall (config, blocks, p, powerups);

        super.score += this.score;
        this.score = 0;
    }

    @Override
    public void draw (Graphics g) {
        if (!active) return;
        Color color = new Color(150,20,20);
        g.setColor(color);
        g.fillOval(posX - size/2, posY - size/2, size, size);

        for (FloatingPoints fp : floatingPoints) fp.draw(g);
    }

}
// ----------------------------------------------------------------------------------------------------------


class Powerup {
    private int timeFalling = 3000, duration = 5000, posX, posY; // time is in milliseconds
    private final int size = 10;
    private powerType type;
    boolean caught = false;
    private int finalPosY;
    public enum powerType {
        pacman,
        spaceInvaders
    }

    public static void spawnPowerup (ArrayList <Powerup> array,GameConfig config, int x, int y) {
        Powerup pwr = new Powerup();
        pwr.posX = x;
        pwr.posY = y;
        pwr.finalPosY = config.HEIGHT_SCREEN - config.SIDE_SIZE / 2 - pwr.size/2;

        Random ran = new Random(); // random power up
        pwr.type = powerType.values()[ran.nextInt(powerType.values().length)];
        array.add(pwr);
    }

    public void draw (Graphics g) {
        if (caught) return; // only draw if its not caught

        Color color = switch (this.type) {
            case pacman -> new Color (250, 200, 0);
            case spaceInvaders -> new Color(200,200,200);
        };

        if (posY >= finalPosY) {
            // Alternate brightness every x seconds
            int timePerBlink = 500; // milliseconds
            boolean isBrightPhase = (timeFalling / timePerBlink) % 2 == 0;

            if (isBrightPhase) color = color.brighter();
        }
        g.setColor(color);
        g.fillOval(posX - size/2, posY - size/2, size, size);
    }

    public static void tick(GameConfig config, ArrayList<Powerup> array, Powerup activePowerup , Player p) {
        for (int i = 0; i < array.size(); i++) {
            Powerup pwr = array.get(i);
            status indTick = pwr.individualTick(config, p);
            if (indTick == status.RemoveSingle) {
                array.remove(i);
                if (activePowerup == pwr) activePowerup = null;
                i--;
            } else if (indTick == status.Caught) {
                array.clear();
                activePowerup = pwr;
                return;
            }
        }
    }

    private enum status {
        Caught,
        RemoveSingle,
        Waiting
    }

    private status individualTick (GameConfig config, Player p) {
        if (caught) { // if caught, will wait the duration time before being removed
            duration -= 1000 / config.DELAY;
            if (duration <= 0) return status.RemoveSingle;
            return status.Waiting;
        }

        if (Utils.touchingPlayer(this.posX, this.posY, this.size, p)) { // when one powerup is caught, clean all others
            caught = true;
            return status.Caught;
        }

        if (timeFalling <= 0) return status.RemoveSingle; //if time runs out, remove

        if (posY >= finalPosY) timeFalling -= 1000 / config.DELAY; // wait on the player's row
        else posY += 10; // falling
        return status.Waiting;
    }
}





// ----------------------------------------------------------------------------------------------------------------------

class Utils {
    public static Color rainbowColor (int time) { // based on the ball combo
        Color color = Color.WHITE;
        if (Ball.Combo.comboCounter > 5) {
            // Alternate color every x seconds
            int colorPhase = (Ball.Combo.comboTimer / time) % 7;
            switch (colorPhase) {
                case 0 -> color = new Color(255,0,0);
                case 1 -> color = new Color(255,174,66);
                case 2 -> color = new Color(255,240,0);
                case 3 -> color = new Color(204,255,0);
                case 4 -> color = new Color(125,249,255);
                case 5 -> color = new Color(42,82,190);
                case 6 -> color = new Color(150,0,130);
            }
        }
        return color;
    }

    public static boolean touchingPlayer (int x, int y, int diameter, Player p) {
        int playerX = p.getPosition().x;
        int playerY = p.getPosition().y;
        int playerWidth = p.getWidth();
        int playerHeight = p.getHeight();

        // Calculate player boundaries
        int playerLeft = playerX - playerWidth / 2;
        int playerRight = playerX + playerWidth / 2;
        int playerTop = playerY - playerHeight / 2;
        int playerBottom = playerY + playerHeight / 2;

        // Check if it intersects the player's hitbox
        boolean horizontalOverlap = (x + diameter / 2 >= playerLeft) && (x - diameter / 2 <= playerRight);
        boolean verticalOverlap = (y + diameter / 2 >= playerTop) && (y- diameter / 2 <= playerBottom);

        return horizontalOverlap && verticalOverlap;
    }
}

/*
* to do:
* menu, add tutorial
* bug fix: block collision
* powerups: the players becomes the spaceship, the blocks hit back; some blocks become the scared ghosts and the ball pacman
*/