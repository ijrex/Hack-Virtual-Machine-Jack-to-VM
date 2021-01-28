package compilationEngine;

import token.*;
import tokenlib.Symbol;

import java.io.IOException;

import compilationEngine.symboltable.*;

public class CompileClassVarDec extends Compile {

  String varKind;
  String varType;

  public CompileClassVarDec() {
    wrapperLabel = "classVarDec";
  }

  protected String handleRoutine() throws IOException {
    switch (pos) {
      case -1:
        return prefix();
      case 0:
        if (passer.isClassVarDec(activeToken)){
          varKind = activeToken.getKeyword().toString();
          return passToken();
        }
        return fail();
      case 1:
        if (passer.isType(activeToken)) {
          if (passer.isIdentifier(activeToken))
            activeToken.setIdentifierCat(IdentifierCat.SYMBOL_DEC);
          varType = activeToken.getValue();
          return passToken();
        }
        return fail();
      case 2:
        if (passer.isIdentifier(activeToken)){
          // @todo: Check for duplicate and throw error
          SymbolEntry symbolEntry = classSymbolTable.add(activeToken, varType, varKind);
          if(symbolEntry.getKind() == SymbolKind.FIELD) {
            numFieldVars++;
          }
          return passSymbolEntry(symbolEntry, true);
        }
        return fail();
      case 3:
        if (passer.matchSymbol(activeToken, Symbol.COMMA))
          return passToken(2);
        if (passer.matchSymbol(activeToken, Symbol.SEMI_COLON))
          return passToken(4);
        return fail();
      case 4:
        return postfix();
      default:
        return fail();
    }
  }

}
