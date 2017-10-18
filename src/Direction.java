/**
 * Directions a snake can move in.
 */
public enum Direction {
    UP, RIGHT, DOWN, LEFT;

    /**
     * Returns the direction opposite of this one.
     *
     * @return The direction opposite of this one.
     */
    public Direction opposite() {
        switch (this) {
            case UP:
                return DOWN;
            case RIGHT:
                return LEFT;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            default:
                throw new IllegalStateException("Unknown Direction: " + this);
        }
    }
}
