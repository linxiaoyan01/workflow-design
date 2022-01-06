package wf.execute.define;


import java.util.Objects;

/**
 * linking from one Node to another
 */
public class Transition {
    public String from;
    public String to;

    public String condition;

    public Transition(String from, String to, String condition) {
        this.from = from;
        this.to = to;
        this.condition = condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transition that = (Transition) o;
        return Objects.equals(from, that.from) &&
                Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}
