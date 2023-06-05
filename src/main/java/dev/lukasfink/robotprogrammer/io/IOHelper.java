package dev.lukasfink.robotprogrammer.io;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.lukasfink.robotprogrammer.components.CodeBlock;
import dev.lukasfink.robotprogrammer.flow.FlowCommand;

import java.util.HashMap;
import java.util.List;

public class IOHelper {

    public static String translateToJson(HashMap<FlowCommand, CodeBlock> codeBlockMap) {
        ExportedCodeBlock[] exportedCodeBlocks = new ExportedCodeBlock[codeBlockMap.values().size()];
        int i = 0;
        for (CodeBlock codeBlock: codeBlockMap.values()) {
            exportedCodeBlocks[i++] = ExportedCodeBlock.fromCodeBlock(codeBlock);
        }

        return new Gson().toJson(exportedCodeBlocks);
    }

    public static List<ExportedCodeBlock> translateToStatements(String json) {
        TypeToken<List<ExportedCodeBlock>> listType = new TypeToken<>(){};

        return new Gson().fromJson(json, listType);
    }

}
