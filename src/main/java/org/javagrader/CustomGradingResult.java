package org.javagrader;

import java.io.*;

/**
 * Exception thrown when a custom grading should be given
 */
public class CustomGradingResult extends Exception implements Serializable {
    
    public final String feedback;
    public final TestResultStatus status;
    public final double grade;
    public final Exception origException;
    public final boolean explicitGrade;

    private CustomGradingResult(TestResultStatus status,
                                double grade,
                                String feedback,
                                Exception origException,
                                boolean explicitGrade) {
        this.status = status;
        this.grade = grade;
        this.feedback = feedback;
        this.origException = origException;
        this.explicitGrade = explicitGrade;
    }

    public CustomGradingResult(TestResultStatus status,double grade,String feedback, Exception origException) {
        this(status, grade, feedback, origException, true);
    }
    
    public CustomGradingResult(TestResultStatus status, double grade, String feedback) {
        this(status, grade, feedback, null, true);
    }

    public CustomGradingResult(TestResultStatus status, double grade) {
        this(status, grade, null, null, true);
    }

    public CustomGradingResult(TestResultStatus status, String feedback) {
        this(status, 0, feedback, null, false);
    }

    public CustomGradingResult(TestResultStatus status, String feedback, Exception origException) {
        this(status, 0, feedback, origException, false);
    }

    public CustomGradingResult(TestResultStatus status, double grade, Exception origException) {
        this(status, grade, null, origException, true);
    }

    public CustomGradingResult(TestResultStatus status, Exception origException) {
        this(status, 0, null, origException, false);
    }

    public CustomGradingResult(TestResultStatus status) {
        this(status, 0, null, null, false);
    }

    @Override
    public String toString() {
        return "CustomGradingResult{" +
                "feedback='" + feedback + '\'' +
                ", status=" + status +
                ", grade=" + grade +
                ", origException=" + origException +
                ", explicitGrade=" + explicitGrade +
                '}';
    }

    public static CustomGradingResult fromSerialization(Object other) {
        byte[] serialized = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bos);
            os.writeObject(other);
            os.close();
            serialized = bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
        ObjectInputStream in = null;
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            in = new ObjectInputStream(bis);
            CustomGradingResult custom = (CustomGradingResult) in.readObject();
            in.close();
            return custom;
        } catch (IOException | ClassNotFoundException e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored){}
            }
            return null;
        }
    }

    public static void main(String[] args) {
        CustomGradingResult custom = new CustomGradingResult(TestResultStatus.SUCCESS, 1, "well done");
        CustomGradingResult other = CustomGradingResult.fromSerialization(custom);
        System.out.println(other);
    }
}
