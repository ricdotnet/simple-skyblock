import dev.ricr.skyblock.utils.NumberUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class NumberUtilsTest {

    static class RandomThing {
        public RandomThing() {}
    }

    @DisplayName("Object to double")
    @Test
    public void testObjectToDouble() {
        var result = 15.0d;
        Object number1 = 15.0;
        Object number2 = "15.0";
        Object randomThing = new RandomThing();

        assertEquals(result, NumberUtils.objectToDouble(number1));
        assertEquals(result, NumberUtils.objectToDouble(number2));
        assertEquals(Double.NaN, NumberUtils.objectToDouble(randomThing));
    }

    @DisplayName("Object to float")
    @Test
    public void testObjectToFloat() {
        var result = 15.0f;
        Object number1 = 15.0;
        Object number2 = "15.0";
        Object randomThing = new RandomThing();

        assertEquals(result, NumberUtils.objectToFloat(number1));
        assertEquals(result, NumberUtils.objectToFloat(number2));
        assertEquals(Float.NaN, NumberUtils.objectToFloat(randomThing));
    }

    @DisplayName("New seed")
    @Test
    public void testNewSeed() {
        var seed = NumberUtils.newSeed();

        assertInstanceOf(Long.class, seed);
        assertInstanceOf(Number.class, seed);
    }
}
