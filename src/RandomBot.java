import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Strategy: Just move randomly and don't collide with itself or go out of bounds
 */
public class RandomBot implements SnakeBot {
    private Point head;

    @Override
    public void initialize(Point start) {
        this.head = start;
    }

    @Override
    public Direction getMove() {
        List<Direction> possibleDirections = new ArrayList<>();
        for (Direction d : Direction.values()) {
            possibleDirections.add(d);
        }
        Collections.shuffle(possibleDirections);

        Direction nextDirection = possibleDirections.get(0);
        for (Direction d : possibleDirections) {
            Point adjacent = Main.get(this.head, d);
            if (Main.isSafe(adjacent)) {
                nextDirection = d;
                this.head = adjacent;
                break;
            }
        }

        return nextDirection;
    }
}
