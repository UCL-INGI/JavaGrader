import org.javagrader.Allow;
import org.javagrader.Forbid;
import org.javagrader.Grade;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;


@Grade
public class InvalidImportTest {

    @Test
    @Grade
    //@Forbid("java.lang.Thread") // thread is always forbidden by default
    public void invalidImportTest1() {
        UnauthorizedCode.staticMethodWithThread();
    }

    @Test
    @Grade
    @Allow("java.lang.Thread")
    @Tag("pass")
    public void validImportTest() {
        UnauthorizedCode.staticMethodWithThread();
    }

    @Test
    @Grade
    public void invalidImportTest2() {
        UnauthorizedCode u = new UnauthorizedCode();
        u.methodWithThread();
    }

    @RepeatedTest(2)
    @Grade
    @Tag("pass")
    public void repeatedValidTest(RepetitionInfo i) {
        System.out.println(i);
    }

    @RepeatedTest(2)
    @Grade
    public void repeatedInvalidTest(RepetitionInfo i) {
        UnauthorizedCode.staticMethodWithThread();
    }

    @RepeatedTest(2)
    @Grade(noRestrictedImport = true)
    @Allow("all")
    @Forbid("java.lang.Thread")
    public void testWithoutSecurity(RepetitionInfo i) {
        // works even though thread is forbidden, as the security is disabled
        UnauthorizedCode.staticMethodWithThread();
    }

    @Grade
    @Test
    public void testMaliciousTable() {
        UnauthorizedCode.printMaliciousTable();
    }

}
