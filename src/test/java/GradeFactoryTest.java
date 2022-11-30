import org.javagrader.Grade;
import org.javagrader.GraderExtension;
import org.javagrader.PrintConstants;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Grade
public class GradeFactoryTest {

    @Grade
    @ParameterizedTest
    @ValueSource(strings = { "a", "b", "c" })
    void testWithString(String argument) {
        assertEquals("b", argument);
    }

    // TODO use argument to run without the custom class loader
    @Grade
    @ParameterizedTest
    @MethodSource("customInputStream")
    void testWithCustomObject(CustomInput argument) {
        assertEquals(0, argument.x);
    }

    public static class CustomInput {
        int x;
        public CustomInput(int x) {
            this.x = x;
        }
    }

    static Stream<CustomInput> customInputStream() {
        return Stream.of(new CustomInput(0), new CustomInput(1));
    }

    @Grade
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 3 })
    void testWithValueSource(int argument) {
        assertEquals(0, argument % 2);
    }

    @RepeatedTest(5)
    @Grade
    public void testTwice(RepetitionInfo info) {
        assertEquals(0, info.getCurrentRepetition() % 2);
    }

}
