import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * This program manages the game of Snake and allows bots to play it.
 *
 * @author Jake Chiang
 * @version v1.2
 */
public class Main {
    /**
     * Width of the play area in number of cells. Either this or GRID_WIDTH must be even if perfect
     * play is to be possible.
     */
    public static final int GRID_HEIGHT = 20;
    /**
     * Height of the play area in number of cells. Either this or GRID_HEIGHT must be even if
     * perfect play is to be possible.
     */
    public static final int GRID_WIDTH = 20;
    /**
     * Size of each grid cell, in pixels.
     */
    public static final int CELL_SIZE = 30;
    /**
     * Maximum delay between game steps in milliseconds.
     */
    public static final int MAX_DELAY = 100;
    /**
     * Default delay between game steps in milliseconds.
     */
    public static final int DEFAULT_DELAY = 50;

    public static final int EMPTY = 0;
    public static final int FOOD = 1;
    public static final int SNAKE = 2;

    private static SimpleGrid grid;
    private static List<SnakeBot> loadedBots;
    private static SnakeBot snake;
    private static int selectedDelay = DEFAULT_DELAY;
    private static Random rand;
    private static Queue<Point> snakeParts;
    private static Point snakeHead;
    private static Point food;
    private static boolean playing;
    private static JLabel score;
    private static JComboBox<String> botSelection;
    private static int foodEaten;
    private static int moves;

    public static void main(String[] args) {
        grid = new SimpleGrid(GRID_WIDTH, GRID_HEIGHT, CELL_SIZE, 1, "Snake AI");
        grid.setGridlineColor(Color.LIGHT_GRAY);
        grid.setColor(FOOD, Color.RED);
        grid.setColor(SNAKE, Color.BLACK);

        loadedBots = new LinkedList<>();
        loadedBots.add(new SpacerBot());
        loadedBots.add(new GreedyTailBot());
        loadedBots.add(new GreedyBot());
        loadedBots.add(new BruteBot());
        loadedBots.add(new RandomBot());
        snake = loadedBots.get(0);

        rand = new Random();
        snakeParts = new LinkedList<>();

        initializeGame();
        initializeGUI();
        run();
    }

    /**
     * Initializes the window's GUI controls.
     */
    private static void initializeGUI() {
        JFrame frame = grid.getFrame();

        ////////// Control Panel \\\\\\\\\\
        JPanel controlPanel = new JPanel();

        // Speed controls
        controlPanel.add(new JLabel("Slow"));
        JSlider speedSlider = new JSlider(JSlider.HORIZONTAL, 0, MAX_DELAY, MAX_DELAY - DEFAULT_DELAY);
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider) e.getSource();
                selectedDelay = MAX_DELAY - source.getValue();
            }
        });
        controlPanel.add(speedSlider);
        controlPanel.add(new JLabel("Fast"));

        // Bot selection dropdown box
        String[] botNames = new String[loadedBots.size()];
        for (int i = 0; i < loadedBots.size(); i++) {
            botNames[i] = loadedBots.get(i).toString();
        }
        botSelection = new JComboBox<>(botNames);
        botSelection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                snake = loadedBots.get(botSelection.getSelectedIndex());
            }
        });
        controlPanel.add(botSelection);

        // Run button
        JButton runButton = new JButton("Run");
        runButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initializeGame();
                playing = true;
            }
        });
        controlPanel.add(runButton);

        // Stop button
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                playing = false;
            }
        });
        controlPanel.add(stopButton);

        ////////// Statistics Display \\\\\\\\\\
        JPanel statsPanel = new JPanel();
        score = new JLabel();
        updateStats();
        statsPanel.add(score);

        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.add(statsPanel, BorderLayout.NORTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    /**
     * Updates the GUI with the current score and number of moves taken by the bot.
     */
    private static void updateStats() {
        double ratio;
        if (foodEaten == 0) {
            ratio = Double.NaN;
        } else {
            ratio = Math.round(100.0 * moves / foodEaten) / 100.0;
        }
        score.setText("Moves: " + moves + " Score: " + foodEaten + " (Ratio: " + ratio + ")");
    }

    /**
     * Initializes the game with the currently loaded snake. Clears the grid and initializes the
     * snake and a food piece to random locations.
     */
    private static void initializeGame() {
        grid.fill(EMPTY);

        int startX = rand.nextInt(GRID_WIDTH);
        int startY = rand.nextInt(GRID_HEIGHT);
        snakeHead = new Point(startX, startY);
        snake.initialize(snakeHead);

        snakeParts.clear();
        snakeParts.add(snakeHead);
        grid.set(snakeHead, SNAKE);

        foodEaten = 0;
        moves = 0;
        addFood();
        playing = true;
    }

    /**
     * Adds a single piece of food to a random open cell in the grid.
     */
    private static void addFood() {
        List<Point> emptyPoints = new LinkedList<>();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                Point p = new Point(x, y);
                if (isEmpty(p)) {
                    emptyPoints.add(p);
                }
            }
        }
        Collections.shuffle(emptyPoints);
        while (!emptyPoints.isEmpty()) {
            Point candidate = emptyPoints.remove(0);
            if (isEmpty(candidate)) {
                food = candidate;
                grid.set(candidate, FOOD);
                return;
            }
        }
    }

    /**
     * Returns whether the given point is an empty cell.
     *
     * @param p The point to check whether it is empty.
     * @return True if the given point is empty, false otherwise.
     */
    private static boolean isEmpty(Point p) {
        return grid.get(p) == EMPTY;
    }

    /**
     * Main game loop. Runs the currently selected algorithm until it loses if playing is enabled.
     */
    private static void run() {
        while (true) {
            while (playing) {
                botSelection.setEnabled(false);

                Direction move = snake.getMove();
                snakeHead = get(snakeHead, move);
                moves++;

                if (snakeHead == null || grid.get(snakeHead) == SNAKE) {
                    playing = false;
                    updateStats();
                    System.out.println("Game over!");
                    break;
                }
                snakeParts.add(snakeHead);

                if (grid.get(snakeHead) == FOOD) {
                    // If it ate food, add another piece
                    addFood();
                    foodEaten++;
                } else {
                    // If it didn't eat food, remove tail of the snake
                    grid.set(snakeParts.remove(), EMPTY);
                }
                grid.set(snakeHead, SNAKE);

                updateStats();
                try {
                    Thread.sleep(selectedDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            botSelection.setEnabled(true);
        }
    }

    /**
     * Returns whether it is safe for a snake to move to the given position.
     *
     * @param p The point to check.
     * @return False if the point is null, out of bounds, or a snake part is at the location, true
     * otherwise.
     */
    public static boolean isSafe(Point p) {
        if (p == null || grid.isOOB(p)) {
            return false;
        }
        return grid.get(p) != SNAKE;
    }

    /**
     * For a given point, returns the point adjacent in the given direction.
     *
     * @param p The point to get the adjacent point of.
     * @param d The direction of the adjacent point to get.
     * @return The point directly adjacent to the given one, in the given direction. If this
     * adjacent point is outside of the play area, returns null instead. If the given direction is
     * null, returns the given point.
     */
    public static Point get(Point p, Direction d) {
        Point adjacent = new Point(p);

        if (d == Direction.UP) {
            adjacent.translate(0, -1);
        } else if (d == Direction.RIGHT) {
            adjacent.translate(1, 0);
        } else if (d == Direction.DOWN) {
            adjacent.translate(0, 1);
        } else if (d == Direction.LEFT) {
            adjacent.translate(-1, 0);
        }

        return grid.isOOB(adjacent) ? null : adjacent;
    }

    /**
     * Returns the location of the food piece.
     *
     * @return The coordinates of the food piece.
     */
    public static Point getFoodPos() {
        return food;
    }

    /**
     * Returns the width of the play area in number of cells.
     *
     * @return The width of the play area in number of cells.
     */
    public static int getGridWidth() {
        return grid.getWidth();
    }


    /**
     * Returns the height of the play area in number of cells.
     *
     * @return The height of the play area in number of cells.
     */
    public static int getGridHeight() {
        return grid.getHeight();
    }
}
