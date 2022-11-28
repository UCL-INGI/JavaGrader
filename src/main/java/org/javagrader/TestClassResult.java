package org.javagrader;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static org.javagrader.PrintConstants.*;
import static org.javagrader.PrintConstants.SeparatorsType.*;

public class TestClassResult {

    private final List<TestMethodResult> resultList;
    private final Class<?> testClass;
    private double sumMaxGradesInTestWithoutIgnored = 0.;
    private double sumGradesInTest = 0.;
    private double sumMaxGradeInTest  = 0.;
    private double maxGrade  = 0.;
    private final String displayName;
    private final boolean isGradeAnnotated;
    private boolean isOneFailure = false;

    public TestClassResult(String displayName, Class<?> testClass) {
        resultList = new ArrayList<>();
        this.testClass = testClass;
        this.displayName = displayName;
        isGradeAnnotated = testClass.isAnnotationPresent(Grade.class);
        if (isGradeAnnotated) {
            Grade g = testClass.getAnnotation(Grade.class);
            maxGrade = g.value();
        }
    }

    public void addTestMethodResult(TestMethodResult result) {
        resultList.add(result);
        // only update the values if the class or the method is graded
        if (isGradeAnnotated || result.isGraded()) {
            sumGradesInTest += result.grade();
            sumMaxGradeInTest += result.maxGrade();
            sumMaxGradesInTestWithoutIgnored += result.isAborted() || result.isDisabled() ? 0. : result.maxGrade();
            isOneFailure = isOneFailure || !(result.status() == TestResultStatus.SUCCESS);
        }
    }

    public String format(PrintConstants.PrintMode mode) {
        if (mode.equals(PrintConstants.PrintMode.NONE))
            return "";
        StringJoiner stringJoiner = new StringJoiner("\n");
        stringJoiner.add(myFormat(mode));
        resultList.forEach(t -> stringJoiner.add(t.format(mode, maxGrade(), sumMaxGradeInTest)));
        return stringJoiner.toString();
    }

    private String myFormat(PrintConstants.PrintMode mode) {
        if (mode.equals(PrintConstants.PrintMode.NONE))
            return "";
        return String.format("%s%s%s%s%s%s%s%s",
                globalPrefix(mode),
                separator(mode, CLASS_PREFIX),
                displayName,
                separator(mode, CLASS_SUFFIX),
                separator(mode, CONTENT),
                status(),
                separator(mode, CONTENT),
                formatGrade(grade(), maxGrade()));
    }

    private String status() {
        if (isOneFailure)
            return statusToIcon(TestResultStatus.FAIL);
        return statusToIcon(TestResultStatus.SUCCESS);
    }

    public double grade() {
        if (isGradeAnnotated) {
            return (sumGradesInTest / sumMaxGradeInTest) * maxGrade;
        } else {
            return sumGradesInTest;
        }
    }

    public double maxGrade() {
        if (isGradeAnnotated) {
            return maxGrade;
        } else {
            return sumMaxGradeInTest;
        }
    }

    public double maxGradeWithoutAborted() {
        if (isGradeAnnotated) {
            return (sumMaxGradesInTestWithoutIgnored / sumMaxGradeInTest) * maxGrade;
        } else {
            return sumMaxGradesInTestWithoutIgnored;
        }
    }

    @Override
    public int hashCode() {
        return displayName.hashCode();
    }
}
