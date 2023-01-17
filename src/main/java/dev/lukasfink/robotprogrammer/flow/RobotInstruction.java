package dev.lukasfink.robotprogrammer.flow;

/**
 * Enum for robot instructions. This represents everything the robot can do.
 */
public enum RobotInstruction {

    INIT("init", false, true),
    FORWARD("forward", true, true),
    BACKWARDS("backwards", true, true),
    TURN_LEFT("turn_left", true, true),
    TURN_RIGHT("turn_right", true, true),
    TERMINATE("terminate", true, false);

    private final String value;
    private final boolean previousAllowed;
    private final boolean nextAllowed;

    RobotInstruction(String value, boolean previousAllowed, boolean nextAllowed) {
        this.value = value;
        this.previousAllowed = previousAllowed;
        this.nextAllowed = nextAllowed;
    }

    public String getValue() {
        return value;
    }

    public boolean isPreviousAllowed() {
        return previousAllowed;
    }

    public boolean isNextAllowed() {
        return nextAllowed;
    }

}
