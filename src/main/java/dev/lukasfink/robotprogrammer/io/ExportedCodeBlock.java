package dev.lukasfink.robotprogrammer.io;

import dev.lukasfink.robotprogrammer.components.CodeBlock;

public class ExportedCodeBlock {

    protected double posX;

    protected double posY;

    protected String instruction;

    public ExportedCodeBlock(double posX, double posY, String instruction) {
        this.posX = posX;
        this.posY = posY;
        this.instruction = instruction;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public static ExportedCodeBlock fromCodeBlock(CodeBlock codeBlock) {
        return new ExportedCodeBlock(codeBlock.getLayoutX(), codeBlock.getLayoutY(), codeBlock.getFlowCommand().getInstruction().getValue());
    }

}
