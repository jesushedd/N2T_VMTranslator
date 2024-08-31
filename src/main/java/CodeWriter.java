import javax.imageio.IIOException;
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

    private void fillMaps(){
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
        oneValueCommands.put("not","!");

    }

    public CodeWriter(Path outFile) throws IOException {
        //open file ready to write
        outWriter = Files.newBufferedWriter(outFile);
        fillMaps();
    }

    CodeWriter(Writer writer){
        outWriter = new BufferedWriter(writer);
        fillMaps();
    }

    public void writeArithmetic(String arithmeticCommand) throws IOException{
        // write comment
        outWriter.write("// " + arithmeticCommand );
        outWriter.newLine();

        //TODO write asm instruction
        String asmInstruction = assembleArithmetic(arithmeticCommand);
        outWriter.write(asmInstruction);
    }

    /*
    Translate an arithmetic command to Hack assembler instructions  St*/
    private String assembleArithmetic(String arithmeticCommand){

        String asmInstructions = null;
        if (oneValueCommands.containsKey(arithmeticCommand)){ //for commands that require only the most top value on Stack
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
        } else if (relationalCommands.containsKey(arithmeticCommand)){
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
                                "@SAVE_TRUE\r\n" +
                                "D;" + relationalCommands.get(arithmeticCommand) + "\r\n" +
                              //if not save false on new stack top value
                                "@SP\r\n" +
                                "A=M-1\r\n" +
                                "M=0\r\n" +

                                "@CONTINUE\r\n" +
                                "0;JMP\r\n" +

                                "(SAVE_TRUE)\r\n" +
                                "@SP\r\n" +
                                "A=M-1\r\n" +
                                "M=1\r\n" +

                                "(CONTINUE)\r\n";
        } else throw new IllegalArgumentException();
        return asmInstructions;

    }

    public void writePushPop (String pushOrPopCommand, String memorySegment, int index) throws IIOException {
        return;

    }

    public void close() throws IOException{
        outWriter.close();
    }


}
