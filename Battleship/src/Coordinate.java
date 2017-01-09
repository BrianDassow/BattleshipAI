
/**
 * A simple object to hold an XY position on a Battleship playing board.
 *
 * @author Seth Dutter - dutters@uwstout.edu
 * @author Seth Berrier - berriers@uwstout.edu
 *
 * @version SPRING.2013
 */
public class Coordinate {

    /**
     * The row (x-value) on the board of this coordinate
     */
    private int x;
    /**
     * The column (y-value) on the board of this coordinate
     */
    private int y;

    /**
     * Construct a coordinate at the given board position
     *
     * @param x The row/x-value of this coordinate.
     * @param y The column/Y-value of this coordinate.
     */
    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Get the row (x-value) of this coordinate on the board.
     *
     * @return The x-value/row of this coordinate.
     */
    public int getX() {
        return this.x;
    }

    /**
     * Get the column (y-value) of this coordinate on the board.
     *
     * @return The y-value/column of this coordinate.
     */
    public int getY() {
        return this.y;
    }

    /**
     * Compare two coordinates to determine if they are equal
     *
     * @param coord The coordinate to compare to
     * @return True if both the x and y values are equal.
     */
    public boolean equals(Coordinate coord) {
        return (this.x == coord.getX() && this.y == coord.getY());
    }

    /**
     * Compare two coordinates to determine which one is 'lower' in value.
     *
     * @param coord The coordinate to compare to
     * @return True if this one is smaller in BOTH the x and y dimensions.
     */
    public boolean lessThan(Coordinate coord) {
        return (this.x < coord.getX() && this.y < coord.getY());
    }

    /**
     * Compare two coordinates to determine which one is 'higher' in value.
     *
     * @param coord The coordinate to compare to
     * @return True if this one is larger in BOTH the x and y dimensions.
     */
    public boolean greaterThan(Coordinate coord) {
        return (this.x > coord.getX() && this.y > coord.getY());
    }

    /**
     * Convert this coordinate to a nicely formatted string.
     *
     * @return A string representing this coordinate
     */
    @Override
    public String toString() {
        return String.format("(%2d, %2d)", x, y);
    }
}
