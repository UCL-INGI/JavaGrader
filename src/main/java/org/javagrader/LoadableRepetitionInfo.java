package org.javagrader;

import org.junit.jupiter.api.RepetitionInfo;

public class LoadableRepetitionInfo implements RepetitionInfo {

    private int current;
    private int total;

    public LoadableRepetitionInfo(int current, int total) {
        this.current = current;
        this.total = total;
    }

    @Override
    public int getCurrentRepetition() {
        return current;
    }

    @Override
    public int getTotalRepetitions() {
        return total;
    }
}
