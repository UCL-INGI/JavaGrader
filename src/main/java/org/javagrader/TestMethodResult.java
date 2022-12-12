package org.javagrader;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.javagrader.PrintConstants.*;
import static org.javagrader.PrintConstants.SeparatorsType.*;
import static org.javagrader.TestResultStatus.*;

public class TestMethodResult {

    private String methodDisplayName;
    private TestResultStatus status;
    private Method testMethod;
    private CustomGradingResult customGradingResult;

    public TestMethodResult(String methodDisplayName, Method testMethod, TestResultStatus status, CustomGradingResult customGradingResult) {
        this.methodDisplayName = methodDisplayName;
        this.testMethod = testMethod;
        this.status = status;
        this.customGradingResult = customGradingResult;
    }

    public TestMethodResult(String methodDisplayName, Method testMethod, TestResultStatus status) {
        this(methodDisplayName, testMethod, status, null);
    }

    public TestResultStatus status() {
        return status;
    }

    public boolean isGraded() {
        return testMethod.isAnnotationPresent(Grade.class);
    }

    public boolean isAborted() {
        return status == ABORTED;
    }

    public boolean isDisabled() {
        return status == DISABLED;
    }
    
    public double maxGrade() {
        if (isGraded()) {
            Grade g = testMethod.getAnnotation(Grade.class);
            return g.value();
        }
        return 1;
    }

    private String comment() {
        if (customGradingResult != null) {
            return '\"' + customGradingResult.feedback + '\"';
        }
        GradeFeedback[] feedbacks = testMethod.getAnnotationsByType(GradeFeedback.class);
        if (feedbacks != null) {
            // get the feedback corresponding to the status
            String msg = Arrays.stream(feedbacks)
                    .filter(f -> f.on() == status)
                    .findFirst()
                    .map(GradeFeedback::message).orElse("");
            if (msg.length() > 1)
                return '\"' + msg + '\"';
            return msg;
        }
        return "";
    }
    
    public double grade() {
        if (customGradingResult != null) {
            if (customGradingResult.explicitGrade)
                return customGradingResult.grade;
            else
                return customGradingResult.status == SUCCESS ? maxGrade() : 0.;
        } else {
            return status == SUCCESS ? maxGrade() : 0.;
        }
    }
    
    public String format(PrintConstants.PrintMode mode, 
                         double classGrade,
                         double sumGradesInTestClass) {
        String sep = separator(mode, CONTENT);
        return String.format("%s%s%s%s%s%s%s%s%s",
                globalPrefix(mode),
                separator(mode, METHOD_PREFIX),
                methodDisplayName,
                separator(mode, METHOD_SUFFIX),
                sep,
                statusToIcon(status),
                sep,
                PrintConstants.formatGrade(scaleTestGrade(classGrade, sumGradesInTestClass),
                        scaleMaxGrade(classGrade, sumGradesInTestClass)),
                commentIfPresent(mode));
    }

    private double scaleTestGrade(double classGrade, double sumGradesInTestClass) {
        return (grade() / sumGradesInTestClass) * classGrade;
    }

    private double scaleMaxGrade(double classGrade, double sumGradesInTestClass) {
        return (maxGrade() / sumGradesInTestClass) * classGrade;
    }

    private String commentIfPresent(PrintConstants.PrintMode mode) {
        String comment = comment();
        if (comment != null && !comment.equals("")) {
            return String.format("%s%s", separator(mode, COMMENT), comment);
        }
        return "";
    }
    
}
