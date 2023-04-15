package ;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static generated.GeneratedTypes.*;

%%

%{
  public _DevtiLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _DevtiLexer
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
