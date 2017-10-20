import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Strategy: Attempt to take the shortest path to the food such that at least 1 cell of separation
 * is maintained between any parts of the snake and the grid boundaries. If no such path exists,
 * then attempt to take the shortest path to the food without the spacing restrictions. If this path
 * doesn't exist, make random moves that don'd collide with itself or go out or bounds.
 */
public class SpacerBot implements SnakeBot {
    protected static class Node implements Comparable<Node> {
        public final Point point; // Position of this node
        public double gScore; // The real cost to reach this node
        public double fScore; // The total cost to reach this node (including heuristic cost)
        public Direction directionToParent;

        public Node(Point point) {
            this.point = point;
            this.gScore = Double.MAX_VALUE;
            this.fScore = Double.MAX_VALUE;
            this.directionToParent = null;
        }

        @Override
        public int hashCode() {
            return this.point.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Node other = (Node) o;
            return this.point.equals(other.point);
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fScore, other.fScore);
        }
    }

    private Point head;
    private List<Point> bodyParts;

    @Override
    public void initialize(Point start) {
        this.head = start;
        this.bodyParts = new LinkedList<>();

        this.bodyParts.add(this.head);
    }

    @Override
    public Direction getMove() {
        Direction nextDirection = pathfindTo(Main.getFoodPos(), true);
        if (nextDirection == null) {
            nextDirection = pathfindTo(Main.getFoodPos(), false);
        }
        if (nextDirection == null) {
            nextDirection = getRandomMove();
        }
        this.head = Main.get(this.head, nextDirection);

        if (this.head == null) {
            return nextDirection;
        }

        // Remove the tail if this move won't eat the food
        if (!this.head.equals(Main.getFoodPos())) {
            this.bodyParts.remove(0);
        }
        this.bodyParts.add(this.head);


        return nextDirection;
    }

    /**
     * Calculates the shortest path from the snake's head to the given target point and returns the
     * direction to move in order to take that path.
     *
     * @param target     The target location to find a path to.
     * @param leaveSpace Whether to place a spacing restriction on the path found that requires at
     *                   least one cell of separation to be kept between snake parts and the grid
     *                   boundaries.
     * @return The direction to move in order to take the shortest path from the snake's head to the
     * given target point.
     */
    private Direction pathfindTo(Point target, boolean leaveSpace) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();
        Map<Point, Node> nodes = new HashMap<>();

        Node startNode = new Node(this.head);
        startNode.gScore = 0;
        startNode.fScore = calcHeuristic(this.head, target);
        openSet.add(startNode);
        nodes.put(startNode.point, startNode);

        while (!openSet.isEmpty()) {
            // Get node with lowest fScore
            Node current = openSet.remove();

            if (current.point.equals(target)) {
                Direction nextDirection = current.directionToParent;
                while (!current.point.equals(this.head)) {
                    nextDirection = current.directionToParent;
                    current = nodes.get(Main.get(current.point, current.directionToParent));
                }
                return nextDirection.opposite();
            }

            closedSet.add(current);

            for (Direction d : Direction.values()) {
                Node neighbor = new Node(Main.get(current.point, d));

                // Ignore already evaluated nodes and ones that aren't traversable
                if (!isSafe(neighbor.point, leaveSpace) || closedSet.contains(neighbor)) {
                    continue;
                }

                // Discover a new Node
                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                    nodes.put(neighbor.point, neighbor);
                }

                // Get the node reference for this point, if one already exists
                if (nodes.containsKey(neighbor.point)) {
                    neighbor = nodes.get(neighbor.point);
                }
                double tentativeGScore = current.gScore + 1;
                if (tentativeGScore < neighbor.gScore) {
                    // This is a better path
                    neighbor.directionToParent = d.opposite();
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = neighbor.gScore + calcHeuristic(neighbor.point, target);

                    // Update this node's position in the priority queue
                    // Note: O(n)
                    if (openSet.contains(neighbor)) {
                        openSet.remove(neighbor);
                        openSet.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the Manhattan distance between the given points.
     *
     * @param p1 First point of the point pair to calculate the heuristic of.
     * @param p2 Second point of the point pair to calculate the heuristic of.
     * @return The Manhattan distance between the two points.
     */
    private double calcHeuristic(Point p1, Point p2) {
        final double tiebreakerWeight = 1.001;
        return tiebreakerWeight * (Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y));
    }

    /**
     * Returns whether the given point is safe to move to given the spacing restrictions of having
     * at least one cell of space between other snake parts and the grid boundaries.
     *
     * @param p          The point to check for safety.
     * @param leaveSpace Whether the given point is required to have at least one cell of space
     *                   between other snake parts and the grid boundaries.
     * @return True if the given point is safe to move to and is safe to move to under the given
     * spacing restriction, false otherwise.
     */
    private boolean isSafe(Point p, boolean leaveSpace) {
        if (!Main.isSafe(p)) {
            return false;
        }
        if (!leaveSpace) {
            return true;
        }

        Point head1 = this.bodyParts.get(this.bodyParts.size() - 1);
        Point head2 = head1;
        if (this.bodyParts.size() >= 2) {
            head2 = this.bodyParts.get(this.bodyParts.size() - 2);
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                Point neighbor = new Point(p.x + x, p.y + y);

                if ((x == 0 && y == 0) || neighbor.equals(head1) || neighbor.equals(head2)) {
                    continue;
                }

                if (!Main.isSafe(neighbor)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns a random movement direction that will not cause this snake to collide with itself or
     * go out of bounds.
     *
     * @return A random movement direction that will not cause this snake to collide with itself or
     * go out of bounds. If no such direction exists, returns UP.
     */
    private Direction getRandomMove() {
        List<Direction> possibleDirections = new ArrayList<>();
        for (Direction d : Direction.values()) {
            possibleDirections.add(d);
        }
        Collections.shuffle(possibleDirections);

        for (Direction d : possibleDirections) {
            Point adjacent = Main.get(this.head, d);
            if (Main.isSafe(adjacent)) {
                return d;
            }
        }

        return Direction.UP;
    }

    @Override
    public String toString() {
        return "SpacerBot";
    }
}
