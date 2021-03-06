package compilationEngine;

import token.*;
import tokenlib.Keyword;
import tokenlib.Symbol;

import java.io.IOException;

import compilationEngine.symboltable.SymbolEntry;
import compilationEngine.vmwriter.VM;

public class CompileTerm extends Compile {

  Compile compileExpressionList;
  Compile compileExpression;
  Compile compileTerm;

  Token lookAheadToken;

  SymbolEntry lookaheadSymbol;
  SymbolEntry subroutineEntry;

  public CompileTerm() {
    routineLabel = "term";
  }

  private String identifierTokenCommand() {
    String location = VM.parseLocation(lookaheadSymbol.getKind());
    return VM.writePush(location, lookaheadSymbol.getKey());
  }

  private String keywordTokenCommand(Keyword keyword) {
    switch (keyword) {
      case TRUE:
        String command = VM.writePush("constant", 0);
        command += "not\n";
        return command;
      case THIS:
        return VM.writePush("pointer", 0);
        case NULL:
        case FALSE:
          return VM.writePush("constant", 0);
      default:
        return null + "\n";
    }
  }

  private String arrayReferenceCommand() {
    String location = VM.parseLocation(lookaheadSymbol.getKind());
    String command = VM.writePush(location, lookaheadSymbol.getKey());
    command += "add\n";
    command += VM.writePop("pointer", 1);
    command += VM.writePush("that", 0);
    return command;
  }

  private String stringCommand(Token token) {

    String command = "";

    String str = token.getValue();
    String trimmedStr = str.substring(1, str.length() -1);
    int strLen = trimmedStr.length();

    command += VM.writePush("constant", strLen);
    command += VM.writeCall("String.new", 1);

    for(int i = 0; i < strLen; i++ ) {
      int c = trimmedStr.charAt(i);
      command += VM.writePush("constant", c);
      command += VM.writeCall("String.appendChar", 2);
    }

    return command;
  }

  private String subroutineCallCommand(int nArgs) {
    String command = "";
    String callClassName = "";

    int runningIndex = lookaheadSymbol.getKey();

    if(runningIndex >= 0){
      String location = VM.parseLocation(lookaheadSymbol.getKind());

      callClassName = lookaheadSymbol.getType();
      command += VM.writePush(location, runningIndex);
      nArgs++;
    } else {
      callClassName = lookaheadSymbol.getName();
    }

    String subroutineCallName;

    if(subroutineEntry != null) {
      subroutineCallName = subroutineEntry.getName();
    } else {
      subroutineCallName = "@todo: handle method call";
    }

    String functionCall = VM.createSubroutineName(callClassName, subroutineCallName);
    command += VM.writeCall(functionCall, nArgs);
    return command;
  }

  protected String handleRoutine() throws IOException {
    switch (pos) {
      case 0:
        if (passer.isIntConst(activeToken))
          return VM.writePush("constant", activeToken.getIntValue()) + passActive(500);
        if (passer.isKeywordConst(activeToken))
          return keywordTokenCommand(activeToken.getKeyword()) + passActive(500);
        if (passer.isStringConst(activeToken)) {
          return stringCommand(activeToken) + passActive(500);
        }
        if (passer.isIdentifier(activeToken)) {
          lookAheadToken = activeToken;
          return passActive();
        }

        if (passer.matchSymbol(activeToken, Symbol.PARENTHESIS_L))
          return passActive(300);

        if (passer.isUnaryOp(activeToken)) {
          lookAheadToken = activeToken;
          return passActive(400);
        }

        return fail();
      case 1:
        Symbol symbol = activeToken.getSymbol();

        switch (symbol) {
          case PERIOD:
            lookaheadSymbol = findSymbolOrStub(lookAheadToken);
            return passActive() + passActive(100);
          case BRACKET_L:
            lookaheadSymbol = findSymbol(lookAheadToken);
            return passActive() + passActive(200);
          case PARENTHESIS_L:
            lookaheadSymbol = findSymbolOrStub(lookAheadToken);
            return passActive() + passActive(102);
          default:
            lookaheadSymbol = findSymbol(lookAheadToken);
            return identifierTokenCommand() + passActive() + endRoutine();
        }
      case 100:
        if (passer.isIdentifier(activeToken)) {
          subroutineEntry = findSymbolOrStub(activeToken);
          return passActive();
        }
        return fail();
      case 101:
        return passActive(passer.matchSymbol(activeToken, Symbol.PARENTHESIS_L));
      case 102:
        if (compileExpressionList == null)
          compileExpressionList = new CompileExpressionList();
        return handleSubroutine(compileExpressionList);
      case 103:
        return passActive(passer.matchSymbol(activeToken, Symbol.PARENTHESIS_R));
      case 104:
        int nArgs = compileExpressionList.getNumArgs();
        return subroutineCallCommand(nArgs) + endRoutine();

      case 200:
        if (compileExpression == null)
          compileExpression = new CompileExpression();
        return handleSubroutine(compileExpression);
      case 201:
        return arrayReferenceCommand() + passActive(passer.matchSymbol(activeToken, Symbol.BRACKET_R));
      case 202:
        return endRoutine();

      case 300:
        if (compileExpression == null)
          compileExpression = new CompileExpression();
        return handleSubroutine(compileExpression);
      case 301:
        return passActive(passer.matchSymbol(activeToken, Symbol.PARENTHESIS_R));
      case 302:
        return endRoutine();

      case 400:
        if (compileTerm == null)
          compileTerm = new CompileTerm();
        return handleSubroutine(compileTerm);
      case 401:
        return VM.writeUnaryOp(lookAheadToken.getSymbol()) + endRoutine();

      case 500:
        return endRoutine();

      default:
        return fail();
    }
  }
}
