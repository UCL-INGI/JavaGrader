import org.javagrader.GraderExtension;
import org.javagrader.Grade;
import org.javagrader.GradeFeedback;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import static org.javagrader.TestResultStatus.*;
import static org.junit.jupiter.api.Assertions.fail;

@Grade
public class GradeTest {

    // should fail as the cpu is busy
    @Test
    @Grade(cpuTimeout = 100, unit = TimeUnit.MILLISECONDS)
    @GradeFeedback(message = "Congrats!", on=SUCCESS)
    @GradeFeedback(message = "Something is wrong", on=FAIL)
    @GradeFeedback(message = "Too slow!", on=TIMEOUT)
    @GradeFeedback(message = "We chose to disable this test", on=DISABLED)
    @GradeFeedback(message = "We chose to abort this test", on=ABORTED)
    public void testTimeoutCPU() {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        long start = thread.getCurrentThreadCpuTime();
        int cnt = 0;
        while (thread.getCurrentThreadCpuTime() - start < 150*1000000L) {
            cnt +=1 ;
        }
    }

    @Grade(value = 2)
    @Test
    void abortedTest() {
        Assumptions.abort();
    }

    @Grade(value = 2)
    @Test
    @Disabled
    void disabledTest() {
        fail();
    }

    @Test
    @Grade
    public void failingTest() {
        fail();
    }

}
