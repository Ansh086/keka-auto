package com.keka.helper;

enum ClockAction {
    CLOCK_IN,
    CLOCK_OUT;

    static ClockAction from(String raw) {
        if ("clock_out".equals(raw)) {
            return CLOCK_OUT;
        }
        return CLOCK_IN;
    }

    String toPrefValue() {
        return this == CLOCK_OUT ? "clock_out" : "clock_in";
    }
}
