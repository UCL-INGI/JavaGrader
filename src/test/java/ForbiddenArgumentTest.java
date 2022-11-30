import org.javagrader.Grade;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class ForbiddenArgumentTest {

    @Grade
    @ParameterizedTest
    @MethodSource("getThreads")
    public void testWithForbidenArg(Thread t) {
        // fails with an error message similar to "Failed to load the test instance as its contains a java.lang.Thread parameter, which is forbidden"
    }

    static Stream<Thread> getThreads() {
        return Stream.of(Thread.currentThread());
    }

}
