package dev.lukasfink.robotprogrammer.flow;

public class FlowCommand {

    public enum State {
        COMPLETE,
        INCOMPLETE,
        WITHOUT_CONNECTIONS
    }

    private final RobotInstruction instruction;

    private FlowCommand previous;
    private FlowCommand next;
    private State state = State.WITHOUT_CONNECTIONS;

    public FlowCommand(RobotInstruction instruction) {
        this.instruction = instruction;
    }

    public RobotInstruction getInstruction() {
        return instruction;
    }

    public boolean hasPrevious() {
        return previous != null;
    }

    public FlowCommand getPrevious() {
        return previous;
    }

    public void setPrevious(FlowCommand previous) {
        this.previous = previous;
    }

    public boolean hasNext() {
        return next != null;
    }

    public FlowCommand getNext() {
        return next;
    }

    public void setNext(FlowCommand next) {
        this.next = next;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getInstructionText() {
        return "${instruction." + instruction.getValue() + "}";
    }

    public boolean isComplete() {
        return true;
    }

}
