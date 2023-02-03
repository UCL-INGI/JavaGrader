import org.javagrader.PrintConstants;
import org.javagrader.TestResultStatus;
import org.junit.jupiter.api.*;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.javagrader.TestResultStatus.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

@Tag("pass")
public class GraderExtensionTest {

    Pattern gradePattern = Pattern.compile(".*(\\d+\\.?\\d*)/(\\d+\\.?\\d*).*", Pattern.CASE_INSENSITIVE);
    ByteArrayOutputStream baos;

    @BeforeEach
    public void setup() {
        baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
    }

    @AfterAll
    static void tearDown() {
        // TODO check results from rst table
    }

    @Test
    @Disabled
    public void customGradingTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(CustomGradingTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(5).succeeded(1).failed(4));
        String s = baos.toString();
        List<TestMethod> methodList = new ArrayList<>();
        methodList.add(new TestMethod("authorizedWithExtraPointTest", SUCCESS,1, 0.2, "Valid as @Grade(custom = true)"));
        methodList.add(new TestMethod("customGradingTestTemplate", SUCCESS,0.2, 0.2, ""));
        methodList.add(new TestMethod("authorizedTest1", SUCCESS, 0.2, 0.2, "Valid as @Grade(custom = true)"));
        methodList.add(new TestMethod("authorizedTest2", SUCCESS, 0.2, 0.2, "Valid as @Grade(custom = true)"));
        methodList.add(new TestMethod("unauthorizedTest", FAIL, 0, 0.2, ""));
        assertRSTTableOneClass(s, "CustomGradingTest", 1.6, 1, 1.6, 1, methodList);
    }

    @Test
    public void displayNameTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(DisplayNameTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(2));
        /*
        String s = baos.toString();
        assertRSTTableOneClass(s, "DisplayNameTest", 1, 1, 1, 1,
                List.of(new TestMethod("A parameterized test with named arguments - 1: An important file", SUCCESS,1, 1, ""),
                        new TestMethod("A parameterized test with named arguments - 2: Another file", SUCCESS,1, 1, "")
                ));

         */
    }

    @Test
    public void forbiddenArgumentTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(ForbiddenArgumentTest.class))
                .execute()
                .testEvents()
                .assertThatEvents()
                .haveExactly(1, event(test("testWithForbidenArg"),
                        finishedWithFailure(instanceOf(ClassNotFoundException.class),
                                message("Failed to load the test instance as its contains a java.lang.Thread parameter, which is forbidden"))));
    }

    @Test
    public void gradeFactoryTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(GradeFactoryTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(11).succeeded(4).aborted(0).failed(7));
    }

    @Test
    public void gradeFromClassTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(GradeFromClassTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(4).succeeded(1).aborted(0).failed(3));

    }

    @Test
    public void gradeFromMethodTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(GradeFromMethodTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(1).aborted(0).failed(1));
    }

    @Test
    public void gradetest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(GradeTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(3).succeeded(0).aborted(1).failed(2));
        /*
        String s = baos.toString();
        System.err.println("--gradetest\n" + s);
        assertRSTTableOneClass(s, "GradeTest", 0, 1, 0, 0.33,
                List.of(new TestMethod("disabledTest", DISABLED,0, 0.33, ""),
                        new TestMethod("failingTest", FAIL,0, 0.17, ""),
                        new TestMethod("abortedTest", ABORTED, 0, 0.33, ""),
                        new TestMethod("testTimeoutCPU", TIMEOUT, 0, 0.17, "Too slow!")));

         */
    }

    @Test
    public void invalidImportTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(InvalidImportTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(10).succeeded(6).aborted(0).failed(4));
    }

    @Test
    public void orderedTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(ConditionalOrderedTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(1).aborted(0).failed(1));
    }

    @Test
    public void multiOrderedTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(MultiConditionalOrderedTest.class))
                .execute()
                .allEvents()
                .assertStatistics(stats -> stats.started(12).succeeded(9).aborted(0).failed(3));
    }

    @Test
    public void runTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(RunTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(3).succeeded(1).aborted(1).failed(1));
    }

    @Test
    public void timeoutTest() {
        EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectClass(TimeoutTest.class))
                .execute()
                .testEvents()
                .assertStatistics(stats -> stats.started(9).succeeded(2).aborted(0).failed(7));
    }

    private class TestMethod {

        String name;
        TestResultStatus status;
        double grade;
        double maxGrade;
        String msg;

        public TestMethod(String name, TestResultStatus status, double grade, double maxGrade, String msg) {
            this.name = name;
            this.status = status;
            this.grade = grade;
            this.maxGrade = maxGrade;
            this.msg =msg;
        }

        public boolean contains(String line) {
            int i = line.indexOf("**→**");
            if (i >= 0) {
                int nameI = line.indexOf(name);
                return nameI < line.indexOf(',') && nameI > i;
            }
            return false;
        }

        public void assertCorrectLine(String line) {
            String pattern =".*\"\\*\\*→\\*\\* " + name + ".*\"," + PrintConstants.statusToIcon(status).replace("*", "\\*") +
                    ",(\\d+\\.?\\d*)/(\\d+\\.?\\d*),?" + formattedMsgPattern() + ".*";
            Pattern testPattern = Pattern.compile(pattern);
            Matcher matcher = testPattern.matcher(line);
            assertTrue(matcher.matches(), "Failed to find pattern \n" + pattern + "in line\n" + line);
        }

        private String formattedMsgPattern() {
            return msg.length() > 0 ? ",\"" + msg + "\"" : "";
        }

    }

    private void assertRSTTableOneClass(String table,
                                        String className,
                                        double total,
                                        double maxTotal,
                                        double totalWoIgnored,
                                        double maxTotalWoIgnored,
                                        List<TestMethod> testCases) {
        String[] tableSplit = table.split("\n");
        // extract the true table printed at the end
        List<String> trueTable = new ArrayList<>();
        boolean withinTable = false;
        for (int i = tableSplit.length-1; i >= 0 ; --i) {
            if (tableSplit[i].contains("--- END GRADE ---")) {
                withinTable = true;
                trueTable.add(tableSplit[i]);
            } else if (tableSplit[i].contains("--- GRADE ---")) {
                withinTable = false;
                trueTable.add(tableSplit[i]);
            } else if (withinTable) {
                trueTable.add(tableSplit[i]);
            }
        }
        Collections.reverse(trueTable);
        // assert name is correct
        String pattern = ".*(\\*\\*" + className + "\\*\\*).*";
        Pattern classPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        System.out.println(classPattern);
        System.out.println(String.join("\n", trueTable));
        assertTrue(classPattern.matcher(trueTable.get(5)).matches());

        // assert every test
        for (TestMethod m: testCases) {
            boolean contained = false;
            for (String line: trueTable) {
                if (m.contains(line)) {
                    //System.out.println("contained + " + m + " on \n" + line);
                    m.assertCorrectLine(line);
                    contained = true;
                    break;
                }
            }
            assertTrue(contained);
        }

        String totalWoIgnoredLine = trueTable.get(trueTable.size()-2);
        Matcher matcher = gradePattern.matcher(totalWoIgnoredLine);
        assertTrue(matcher.matches());
        assertEquals(totalWoIgnored, Double.parseDouble(matcher.group(1)), 0.01);
        assertEquals(maxTotalWoIgnored, Double.parseDouble(matcher.group(2)), 0.01);

        String totalLine = trueTable.get(trueTable.size()-3);
        matcher = gradePattern.matcher(totalLine);
        assertTrue(matcher.matches());
        assertEquals(total, Double.parseDouble(matcher.group(1)), 0.01);
        assertEquals(maxTotal, Double.parseDouble(matcher.group(2)), 0.01);

        // TODO assert is printable as rst table
    }

}
