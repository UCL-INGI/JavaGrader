package org.javagrader;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Holds constants related to the printing of values
 */
public final class PrintConstants {

    public enum PrintMode {
        NONE,   // prints nothing
        RST,    // prints the results as an RST table
        NORMAL, // prints as normal text
    }

    public final static String RSTBold = "**";

    /**
     * Types of separators when printing results
     */
    public enum SeparatorsType {
        CLASS_PREFIX,   // added before class result declaration
        CLASS_SUFFIX,
        METHOD_PREFIX,  // added before test method result declaration
        METHOD_SUFFIX,  // added before test method result declaration
        CONTENT,        // separators for table / structured content
        COMMENT         // separator to add in case of comment for a test
    }

    /**
     * Separators to use when printing results
     * 
     * @param mode printing mode
     * @param sep type of separator
     * @return separator to use depending on the printing mode and the type of separation
     */
    public static String separator(PrintMode mode, SeparatorsType sep) {
        switch (mode) {
            case NONE: {return "";} // empty string when no printing must be done
            case NORMAL: {
                switch (sep) {
                    case CLASS_PREFIX: {return "- ";}
                    case METHOD_PREFIX: {return "\t";}
                    case CONTENT: {return " ";}
                    case COMMENT: {return "\n\t\t";}
                    case CLASS_SUFFIX: {return "";}
                    case METHOD_SUFFIX: {return "";}
                }
            }
            case RST: {
                switch (sep) {
                    case CLASS_PREFIX: {return "\"**";} // for bold
                    case CLASS_SUFFIX: {return "**\"";}
                    case METHOD_PREFIX: {return "\"**→** ";}
                    case METHOD_SUFFIX: {return "\"";}
                    case CONTENT: {return ",";}
                    case COMMENT: {return ",";}
                }
            }
        }
        throw new IllegalArgumentException("unrecognized PrintMode" + mode + " with " + sep);
    }

    private static final DecimalFormat df = initDF();

    private static DecimalFormat initDF() {
        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.ENGLISH);
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.getGroupingSeparator();
        return new DecimalFormat("0.##", otherSymbols);
    }

    public static String format(double d) {
        return df.format(d);
    }

    public static String statusToIcon(TestResultStatus status) {
        switch (status) {
            case DISABLED: {
                return "\uD83D\uDEAB Disabled";
            }
            case FAIL: {
                return "❌ **Failed**";
            }
            case SUCCESS: {
                return "✅️ Success";
            }
            case ABORTED: {
                return "\uD83D\uDEAB Aborted";
            }
            case TIMEOUT: {
                return "\uD83D\uDD51 **Timeout**";
            }
            default: {
                throw new IllegalArgumentException("unrecognized status" + status);
            }
        }
    }

    public static String formatGrade(double grade, double maxGrade) {
        return String.format("%s/%s", format(grade), format(maxGrade));
    }

    public static final String RSTTableHeader = ".. csv-table::\n" +
            "    :header: \"Test\", \"Status\", \"Grade\", \"Comment\"\n" +
            "    :widths: auto\n"+
            "    ";

    public static String globalPrefix(PrintMode mode) {
        if (mode == PrintMode.RST)
            return "    ";
        return "";
    }

}
