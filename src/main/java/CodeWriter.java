import org.apache.commons.io.FilenameUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class CodeWriter {

    private final BufferedWriter outWriter;

    /*AArithmetic and Logic Commands that operate 2 values*/
    private final Map<String, String> arithmeticLogicCommands = new HashMap<>();

    /*AArithmetic and Logic Commands that operate 1 value*/
    private final Map<String, String> oneValueCommands = new HashMap<>();

    /*Relational Commands that operate 2 values*/
    private final Map<String, String> relationalCommands = new HashMap<>();

    /*Mapping access pointer for memory segments : local, argument, temp, this, that*/
    private final Map<String, String> segmentPointers = new HashMap<>();

    private final String className;

    private int relationalCounter;



    public CodeWriter(Path outFile) throws IOException {
        //open file ready to write
        outWriter = Files.newBufferedWriter(outFile);
        //save class name
        className = FilenameUtils.getBaseName(outFile.toString());
        relationalCounter = 0;
        fillMaps();

    }


    /*Constructor for testing*/
    CodeWriter(Writer writer, String nameOfClass) {
        outWriter = new BufferedWriter(writer);
        className = nameOfClass;
        relationalCounter = 0;
        fillMaps();
    }

    private void fillMaps() {
        //fill arithmetic and logic mapping
        arithmeticLogicCommands.put("add", "+");
        arithmeticLogicCommands.put("sub", "-");
        arithmeticLogicCommands.put("and", "&");
        arithmeticLogicCommands.put("or", "|");

        //fill relational mapping
        relationalCommands.put("eq", "JEQ");
        relationalCommands.put("gt", "JGT");
        relationalCommands.put("lt", "JLT");

        //fil one value mapping
        oneValueCommands.put("neg", "-");
        oneValueCommands.put("not", "!");

        //fill memory segments mapping
        segmentPointers.put("argument", "@ARG");
        segmentPointers.put("local", "@LCL");
        //segmentPointers.put("static", "??");
        segmentPointers.put("this", "@THIS");
        segmentPointers.put("that", "@THAT");
        segmentPointers.put("temp", "@5");

    }

    public void writeArithmetic(String arithmeticCommand) throws IOException {
        // write comment
        outWriter.write("// " + arithmeticCommand);
        outWriter.newLine();

        //write asm instructions
        String asmInstruction = assembleArithmetic(arithmeticCommand);
        outWriter.write(asmInstruction);
    }

    /*
    Translate an arithmetic command to Hack assembler instructions  St*/
    private String assembleArithmetic(String arithmeticCommand) {

        String asmInstructions;
        if (oneValueCommands.containsKey(arithmeticCommand)) { //for commands that require only the most top value on Stack
            asmInstructions = "@SP\r\n" + //select stack pointer
                    "A=M-1\r\n" + // dereference it and subtract 1 , select top stack top value
                    "M=" + oneValueCommands.get(arithmeticCommand) + "M\r\n";  // update top value with new value commanded
        } else if (arithmeticLogicCommands.containsKey(arithmeticCommand)) { //for commands that require the 2 most top values on Stack

            asmInstructions = //get top value (y)
                    "@SP\r\n" +  //select stack pointer
                            "AM=M-1\r\n" + //dereference it and subtract 1, select top stack value ,update stack pointer by -1
                            "D=M\r\n" + //save top stack value

                            "@SP\r\n" + // select new stack pointer (top value)
                            "A=M-1\r\n" + // dereference it and subtract 1,  this is the second to top value from stack (x), select it

                            // execute operation and save it
                            "M=M" + arithmeticLogicCommands.get(arithmeticCommand) + "D\r\n"; // save result of operation
        } else if (relationalCommands.containsKey(arithmeticCommand)) {
            asmInstructions = //get top value (y)
                            "@SP\r\n" +  //select stack pointer
                            "AM=M-1\r\n" + //dereference it and subtract 1, select top stack value ,update stack pointer by -1
                            "D=M\r\n" + //save top stack value
                            //get x
                            "@SP\r\n" +
                            "A=M-1\r\n" +
                            //save comparison
                            "D=M-D\r\n" +
                            // if condition given is true; jump to save true
                            "@SAVE_TRUE" + relationalCounter +"\r\n" +
                            "D;" + relationalCommands.get(arithmeticCommand) + "\r\n" +
                            //if not save false on new stack top value
                            "@SP\r\n" +
                            "A=M-1\r\n" +
                            "M=0\r\n" +

                            "@CONTINUE"+ relationalCounter +"\r\n" +
                            "0;JMP\r\n" +

                            "(SAVE_TRUE" +  relationalCounter +")\r\n" +
                            "@SP\r\n" +
                            "A=M-1\r\n" +
                            "M=-1\r\n" +

                            "(CONTINUE" + relationalCounter + ")\r\n";
            relationalCounter++;
        } else throw new IllegalArgumentException();
        return asmInstructions;

    }

    public void writePushPop(String pushOrPopCommand, String memorySegment, int index) throws IOException {
        //write comment
        outWriter.write("//" + pushOrPopCommand + " " + memorySegment + " " + index + "\r\n");

        String asmInstructions;

        if (segmentPointers.containsKey(memorySegment)) {
            asmInstructions = assembleUsingPointerSegment(pushOrPopCommand, memorySegment, index);
        } else if (memorySegment.equals("constant")){
            asmInstructions = assembleConstant(index);
        } else if (memorySegment.equals("static")) {
            asmInstructions = assembleStatic(pushOrPopCommand, index);

        } else if (memorySegment.equals("pointer")){
            asmInstructions = assemblePointer(pushOrPopCommand, index);
        } else throw new IllegalArgumentException();

        //write asm instructions
        outWriter.write(asmInstructions);


    }

    private String assemblePointer(String pushOrPopCommand, int index) {
        String asmInstructions ;
        if (!(index == 0 | index == 1)) throw  new IllegalArgumentException();

        String thisOrThat = index == 0 ? "@THIS\r\n" : "@THAT\r\n";
        if (pushOrPopCommand.equals("pop")){
            asmInstructions =   "@SP\r\n" +
                                "AM=M-1\r\n" +
                                "D=M\r\n" +
                                thisOrThat +
                                "M=D\r\n";
        } else if (pushOrPopCommand.equals("push")) {
            asmInstructions =   thisOrThat +
                                "D=M\r\n" +

                                "@SP\r\n" +
                                "A=M\r\n" +
                                "M=D\r\n" +

                                "@SP\r\n" +
                                "M=M+1\r\n";
        } else throw new IllegalArgumentException();
        return  asmInstructions;
    }

    private String assembleStatic(String pushOrPop, int index) {
        String asmInstruction;
        if (pushOrPop.equals("pop")){
            asmInstruction =    "@SP\r\n" +
                                "AM=M-1\r\n" +
                                "D=M\r\n" +
                                //save on @className.i
                                "@" + className + "." + index + "\r\n" +
                                "M=D\r\n";
        } else if (pushOrPop.equals("push")) {
            asmInstruction =    "@" + className + "." + index + "\r\n" + //get vale from static i
                                "D=M\r\n" +
                                "@SP\r\n" +
                                "A=M\r\n" +
                                "M=D\r\n" +
                                "@SP\r\n" +
                                "M=M+1\r\n";
        } else throw new IllegalArgumentException();
        return asmInstruction;
    }



    private String assembleConstant(int index) {
        return                      "@" + index + "\r\n" +
                                    "D=A\r\n" +
                                    "@SP\r\n" +
                                    "A=M\r\n" +
                                    "M=D\r\n" +
                                    "@SP\r\n" +
                                    "M=M+1\r\n";
    }

    private String assembleUsingPointerSegment(String popPush, String memorySegment, int index) {
        String asmInstructions;
        String deReference = memorySegment.equals("temp") ? "A" : "M";

        if (popPush.equals("pop")){
            asmInstructions =   "@" + index + "\r\n" +
                                "D=A\r\n" +
                                segmentPointers.get(memorySegment) + "\r\n" +
                                "D=" + deReference + "+D\r\n" +
                                "@addr\r\n" +
                                "M=D\r\n" +

                                "@SP\r\n" +
                                "AM=M-1\r\n" +
                                "D=M\r\n" +

                                "@addr\r\n" +
                                "A=M\r\n" +
                                "M=D\r\n";
        } else if (popPush.equals("push")){
            asmInstructions =   "@" + index + "\r\n" +
                                "D=A\r\n" +
                                segmentPointers.get(memorySegment) + "\r\n" +
                                "A="+ deReference + "+D\r\n" +
                                "D=M\r\n" +

                                "@SP\r\n" +
                                "A=M\r\n" +
                                "M=D\r\n" +

                                "@SP\r\n" +
                                "M=M+1\r\n";
        } else throw new IllegalArgumentException();
        return  asmInstructions;
    }


    public void close() throws IOException{
        outWriter.close();
    }


}
