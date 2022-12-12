import org.javagrader.Grade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Grade
public class DisplayNameTest {

    @Grade
    @DisplayName("A parameterized test with named arguments")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("namedArguments")
    void testWithNamedArguments(String path) {

    }

    static Stream<Arguments> namedArguments() {
        return Stream.of(
                arguments(named("An important file", "path1")),
                arguments(named("Another file", "path2"))
        );
    }

}
