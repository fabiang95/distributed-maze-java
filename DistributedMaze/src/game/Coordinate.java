package game;

import java.io.Serializable;
import java.util.Objects;

// from https://www.baeldung.com/java-custom-class-map-key#:~:text=Custom%20Key%20Classes,'t%20change%20(Internal%20Consistency)
public class Coordinate implements Serializable {
    private final int x;
    private final int y;
    private int hashCode;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
        this.hashCode = Objects.hash(x, y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Coordinate that = (Coordinate) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}

