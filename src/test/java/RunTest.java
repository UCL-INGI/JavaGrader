import org.javagrader.Grade;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

@Grade(10)
public class RunTest {

    @Test
    @Grade(2)
    @Tag("pass")
    void passingTest() {

    }

    @Test
    @Grade
    void failingTest() {
        fail();
    }

    @Test
    @Grade
    @Disabled
    void disabledTest() {

    }

    @Test
    @Grade(value = 1)
    void abortedTest() {
        Assumptions.abort();
    }

}
