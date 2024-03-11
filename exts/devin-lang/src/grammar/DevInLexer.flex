package cc.unitmesh.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static cc.unitmesh.language.psi.DevInTypes.*;
import com.intellij.psi.TokenType;

%%

%{
  public _DevInLexer() {
    this((java.io.Reader)null);
  }
%}

%class DevInLexer
%class _DevInLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType
%eof{  return;
%eof}

EOL=\R
WHITE_SPACE=\s+

IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*
TEXT_SEGMENT=[^[@\\$]_a-zA-Z0-9]+
WS=[ \t\n\x0B\f\r]
NEWLINE=\n|\r\n

%%
<YYINITIAL> {
  {WHITE_SPACE}        { return TokenType.WHITE_SPACE; }

  "$"                  { return DOLLAR; }
  "@"                  { return AT; }
  "/"                  { return SLASH; }

  {IDENTIFIER}         { return IDENTIFIER; }
  {TEXT_SEGMENT}       { return TEXT_SEGMENT; }
  {WS}                 { return WS; }
  {NEWLINE}            { return NEWLINE; }

}

[^]                                                         { return TokenType.BAD_CHARACTER; }
