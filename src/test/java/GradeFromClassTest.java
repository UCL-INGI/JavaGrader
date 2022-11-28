import org.javagrader.GraderExtension;
import org.javagrader.Grade;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.fail;

@Grade(value = 5, cpuTimeout = 200, unit = TimeUnit.MILLISECONDS)
public class GradeFromClassTest {

    @Test
    @Grade(2)
    @Tag("pass")
    public void passingTest() {
        // overriding default grade value
    }

    @Test
    public void failingTest() {
        fail();
    }

    // the timeout is retrieved from the class and the test fails
    @Test
    public void timeoutComingFromClass() {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        long start = thread.getCurrentThreadCpuTime();
        int cnt = 0;
        while (thread.getCurrentThreadCpuTime() - start < 300*1000000L) {
            cnt +=1 ;
        }
    }

    // the timeout is overridden from the class and the test fails
    @Test
    @Grade(cpuTimeout = 120, unit = TimeUnit.MILLISECONDS)
    public void overrideTimeout() {
        ThreadMXBean thread = ManagementFactory.getThreadMXBean();
        long start = thread.getCurrentThreadCpuTime();
        int cnt = 0;
        while (thread.getCurrentThreadCpuTime() - start < 150*1000000L) {
            cnt +=1 ;
        }
    }

}
