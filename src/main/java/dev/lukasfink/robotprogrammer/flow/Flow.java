package dev.lukasfink.robotprogrammer.flow;

import dev.lukasfink.robotprogrammer.util.Translator;

import java.util.LinkedList;
import java.util.List;

public class Flow {

    private final List<FlowCommand> commands;
    private final FlowCommand startCommand;

    public Flow() {
        commands = new LinkedList<>();

        startCommand = new FlowCommand(RobotInstruction.INIT);
        commands.add(startCommand);
    }

    public FlowCommand getStartCommand() {
        return startCommand;
    }

    public boolean isComplete() {
        FlowCommand currentCommand = getStartCommand();
        while (currentCommand.hasNext()) {
            if (currentCommand.getState() != FlowCommand.State.COMPLETE) {
                return false;
            }

            currentCommand = currentCommand.getNext();
        }

        return true;
    }

    public void reset() {
        commands.clear();
        commands.add(startCommand);
        startCommand.setNext(null);
    }

    public int count() {
        FlowCommand currentCommand = getStartCommand();
        int counter = 1;
        while (currentCommand.hasNext()) {
            currentCommand = currentCommand.getNext();
            counter += 1;
        }

        return counter;
    }

    public void addCommand(FlowCommand command) {
        commands.add(command);
    }

    public void removeCommand(FlowCommand command) {
        commands.remove(command);
    }

    public String generateSourceCode() {
        FlowCommand currentCommand = getStartCommand();
        StringBuilder sourceCode = new StringBuilder(currentCommand.getInstructionText()).append("\n");
        while (currentCommand.hasNext()) {
            currentCommand = currentCommand.getNext();
            sourceCode.append(currentCommand.getInstructionText()).append("\n");
        }

        return Translator.translate(sourceCode.toString());
    }

    public void updateStates() {
        for (FlowCommand command: commands) {
            FlowCommand currentCommand = command;
            boolean connectsToStart = currentCommand == startCommand;
            if (!connectsToStart) {
                while (currentCommand.hasPrevious()) {
                    currentCommand = currentCommand.getPrevious();
                }

                if (currentCommand == startCommand) {
                    connectsToStart = true;
                }
            }

            currentCommand = command;
            while (currentCommand.hasNext()) {
                currentCommand = currentCommand.getNext();
            }

            boolean connectsToEnd = currentCommand.getInstruction() == RobotInstruction.TERMINATE;

            if (connectsToStart && connectsToEnd && command.isComplete()) {
                command.setState(FlowCommand.State.COMPLETE);
            } else if (connectsToStart) {
                command.setState(FlowCommand.State.INCOMPLETE);
            } else {
                command.setState(FlowCommand.State.WITHOUT_CONNECTIONS);
            }
        }
    }

}
