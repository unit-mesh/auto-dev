package cc.unitmesh.devti.language.parser;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static cc.unitmesh.devti.language.psi.SilveryTypes.*;

%%

%{
  public _SilveryLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _SilveryLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

EOL=\R
WHITE_SPACE=\s+


%%
<YYINITIAL> {
  {WHITE_SPACE}        { return WHITE_SPACE; }

  "integer"            { return INTEGER; }
  "INTEGER_LITERAL"    { return INTEGER_LITERAL; }


}

[^] { return BAD_CHARACTER; }
