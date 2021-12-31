package wf;

import java.util.Objects;


public class ForkToken {
    public String id;
    //对应的fork
    public String forkID;
    //对应的fork一共产生了多少个token
    public int total;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForkToken forkToken = (ForkToken) o;
        return total == forkToken.total &&
                Objects.equals(id, forkToken.id) &&
                Objects.equals(forkID, forkToken.forkID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, forkID, total);
    }
}
