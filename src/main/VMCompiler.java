import loadfile.*;
import tokenizer.*;

class VMCompiler {

  public static void main(String[] args) {

    String dir = "../../test-files/Compiler";

    LoadFiles files = new LoadFiles(dir, "jack", true);

    Tokenizer tokenizer = new Tokenizer(files);
  }
}
