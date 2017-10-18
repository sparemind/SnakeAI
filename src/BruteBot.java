import java.awt.Point;

/**
 * Strategy: Travel the same Hamiltonian cycle forever.
 * <p>
 * This bot is guaranteed to win if the playing grid dimensions are even.
 */
public class BruteBot implements SnakeBot {
    private Point head;

    @Override
    public void initialize(Point start) {
        this.head = start;
    }

    @Override
    public Direction getMove() {
        Direction nextMove;
        int parity = this.head.y % 2;

        if (this.head.x == 0) {
            if (this.head.y == 0) {
                nextMove = Direction.RIGHT;
            } else {
                nextMove = Direction.UP;
            }
        } else {
            if (parity == 0) {
                if (this.head.x == Main.getGridWidth() - 1) {
                    nextMove = Direction.DOWN;
                } else {
                    nextMove = Direction.RIGHT;
                }
            } else { // parity == 1
                if (this.head.x == 1) {
                    if (this.head.y == Main.getGridHeight() - 1) {
                        nextMove = Direction.LEFT;
                    } else {
                        nextMove = Direction.DOWN;
                    }
                } else {
                    nextMove = Direction.LEFT;
                }
            }
        }

        this.head = Main.get(this.head, nextMove);
        return nextMove;
    }

    @Override
    public String toString() {
        return "BruteBot";
    }
}
