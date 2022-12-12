import org.javagrader.CustomGradingResult;
import org.javagrader.Grade;
import org.javagrader.TestResultStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Grade
public class CustomGradingTest {

    @Grade
    @Test
    public void unauthorizedTest() throws Exception {
        throw new CustomGradingResult(TestResultStatus.SUCCESS, 1, "Not valid unless @Grade(custom = true)");
    }

    @Grade(custom = true)
    @Test
    public void authorizedTest1() throws Exception {
        throw new CustomGradingResult(TestResultStatus.SUCCESS, 1, "Valid as @Grade(custom = true)");
    }

    @Grade(custom = true)
    @Test
    public void authorizedTest2() throws Exception {
        throw new CustomGradingResult(TestResultStatus.SUCCESS, "Valid as @Grade(custom = true)");
    }

    @Grade(custom = true)
    @Test
    public void authorizedWithExtraPointTest() throws Exception {
        throw new CustomGradingResult(TestResultStatus.SUCCESS, 5, "Valid as @Grade(custom = true)");
    }

    @Grade(custom = true)
    @Test
    @Tag("pass")
    public void customGradingTestTemplate() throws Exception {
        int result = -1;
        try {
            // student code
            if (false) {
                throw new CustomGradingResult(TestResultStatus.SUCCESS);
            }
        } catch (CustomGradingResult e) {
            throw new CustomGradingResult(TestResultStatus.FAIL, "Well tried but we are protected against that");
        }
        if (result == 0) {
            throw new CustomGradingResult(TestResultStatus.FAIL, "Not successfull yet");
        } else if (result > 10) {
            throw new CustomGradingResult(TestResultStatus.SUCCESS,2,  "Well done, you deserved an extra point");
        } else if (result > 5) {
            throw new CustomGradingResult(TestResultStatus.SUCCESS,1,  "Well done");
        }
        //by default, if you throw nothing, it's SUCCESS with the maximum grade
    }

}
