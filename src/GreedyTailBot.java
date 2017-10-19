import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * Strategy: Take the shortest path to the food. If no path exists, move towards the oldest body
 * part that can be moved to. If the snake would reach this oldest part in fewer moves than it would
 * take the part to disappear, the snake will attempt to stall for time by moving to the farthest
 * point away from its current location.
 */
public class GreedyTailBot implements SnakeBot {
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
    private Queue<Point> bodyParts;
    // The number of moves left until a body part disappears.
    private Map<Point, Integer> ages;
    // The oldest body part that can be moved to
    private Point oldestFoundPart;
    // Path length of the last pathfinding
    private int pathLength;

    @Override
    public void initialize(Point start) {
        this.head = start;
        this.bodyParts = new LinkedList<>();
        this.ages = new HashMap<>();

        this.bodyParts.add(this.head);
        this.ages.put(this.head, 1);
    }

    @Override
    public Direction getMove() {
        Direction nextDirection = pathfindTo(Main.getFoodPos());
        if (nextDirection == null) {
            nextDirection = pathfindTo(this.oldestFoundPart);
            if (this.pathLength != 1 && this.pathLength < this.ages.get(this.oldestFoundPart)) {
                nextDirection = pathfindTo(getFarthestPoint());
            }
        }
        this.head = Main.get(this.head, nextDirection);

        // If this move won't eat the food: Remove the tail and decrease the age of all body parts
        if (!this.head.equals(Main.getFoodPos())) {
            // Remove the tail
            Point tail = this.bodyParts.remove();
            this.ages.remove(tail);

            // Update all body part ages
            for (Point part : this.ages.keySet()) {
                this.ages.put(part, this.ages.get(part) - 1);
            }
        }
        this.bodyParts.add(this.head);
        this.ages.put(this.head, this.bodyParts.size() - 1);

        return nextDirection;
    }

    /**
     * Calculates the shortest path from the snake's head to the given target point and returns the
     * direction to move in order to take that path. Also determines the oldest body part of the
     * snake that can be moved to.
     *
     * @param target The target location to find a path to.
     * @return The direction to move in order to take the shortest path from the snake's head to the
     * given target point.
     */
    private Direction pathfindTo(Point target) {
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Set<Node> closedSet = new HashSet<>();
        Map<Point, Node> nodes = new HashMap<>();
        this.oldestFoundPart = this.head;

        Node startNode = new Node(this.head);
        startNode.gScore = 0;
        if (target == null) {
            System.out.println("T");
        }
        if (this.head == null) {
            System.out.println("H");
        }
        startNode.fScore = calcHeuristic(this.head, target);
        openSet.add(startNode);
        nodes.put(startNode.point, startNode);

        while (!openSet.isEmpty()) {
            // Get node with lowest fScore
            Node current = openSet.remove();

            if (current.point.equals(target)) {
                Direction nextDirection = current.directionToParent;
                this.pathLength = 0;
                while (!current.point.equals(this.head)) {
                    this.pathLength++;
                    nextDirection = current.directionToParent;
                    current = nodes.get(Main.get(current.point, current.directionToParent));
                }
                return nextDirection.opposite();
            }

            closedSet.add(current);

            for (Direction d : Direction.values()) {
                Node neighbor = new Node(Main.get(current.point, d));

                // Ignore already evaluated nodes and ones that aren't traversable, unless it is the
                // target point
                if ((!Main.isSafe(neighbor.point) || closedSet.contains(neighbor)) && !target.equals(neighbor.point)) {
                    // Update the oldest body part found.
                    if (this.bodyParts.contains(neighbor.point)) {
                        // System.out.println(neighbor.point + "," + this.oldestFoundPart + "," + this.head + "," + this.ages);
                        if (this.ages.containsKey(neighbor.point) && (this.ages.get(neighbor.point) < this.ages.get(this.oldestFoundPart))) {
                            this.oldestFoundPart = neighbor.point;
                        }
                    }
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

    /**
     * Returns the point in the play area that would take the most number of moves to get to.
     *
     * @return The point in the play area that would take the most number of moves to get to.
     */
    private Point getFarthestPoint() {
        Queue<Point> openSet1 = new LinkedList<>();
        Queue<Point> openSet2 = new LinkedList<>();
        Set<Point> closedSet = new HashSet<>();
        openSet1.add(this.head);

        Point farthest = this.head;
        while (!openSet1.isEmpty()) {
            while (!openSet1.isEmpty()) {
                Point current = openSet1.remove();
                closedSet.add(current);

                for (Direction d : Direction.values()) {
                    Point neighbor = Main.get(current, d);
                    if (Main.isSafe(neighbor) && !closedSet.contains(neighbor)) {
                        if (!openSet2.contains(neighbor)) {
                            openSet2.add(neighbor);
                            farthest = neighbor;
                        }
                    }
                }
            }
            while (!openSet2.isEmpty()) {
                openSet1.add(openSet2.remove());
            }
        }

        return farthest;
    }

    @Override
    public String toString() {
        return "GreedyTailBot";
    }
}
