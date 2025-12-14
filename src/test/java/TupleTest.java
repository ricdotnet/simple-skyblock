import dev.ricr.skyblock.utils.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TupleTest {

    @DisplayName("Tuple is valid")
    @Test
    public void testTuple() {
        var tuple = new Tuple<>("Hello", 15);

        assertEquals("Hello", tuple.getFirst());
        assertEquals(15, tuple.getSecond());
    }

}
