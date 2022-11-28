import org.javagrader.GraderExtension;
import org.javagrader.Grade;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(GraderExtension.class)
public class GradeFromMethodTest {

    @Grade
    @Test
    public void failingTest() {
        fail();
    }

    @Grade
    @Test
    @Tag("pass")
    public void passingTest() {

    }

}
