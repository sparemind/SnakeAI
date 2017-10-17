import java.awt.Point;

/**
 * The interface for a bot.
 */
public interface SnakeBot {
    /**
     * Initializes this bot with the snake's starting location.
     *
     * @param start The starting coordinates of the snake in the play area.
     */
    void initialize(Point start);

    /**
     * Returns the direction the snake will move next.
     *
     * @return The direction of this snake's next move.
     */
    Direction getMove();
}
