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

%s CONTEXT_BLOCK
%s ID_SUFFIX

IDENTIFIER=[a-zA-Z0-9]([_\-a-zA-Z0-9]*)
REF_BLOCK=("@" {IDENTIFIER} )
TEXT_SEGMENT=[^$/@]+
NEWLINE=\n|\r\n

%{
    private IElementType contextBlock() {
        yybegin(YYINITIAL);

        String text = yytext().toString();
        System.out.println("contextBlock: " + text);

        return TEXT_SEGMENT;
    }
%}

%%
<YYINITIAL> {
  {REF_BLOCK}          { return REF_BLOCK; }
  {TEXT_SEGMENT}       { return TEXT_SEGMENT; }
  {NEWLINE}            { return NEWLINE; }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<CONTEXT_BLOCK> {
  [$/@]                { yybegin(YYINITIAL); return contextBlock(); }
  [^]                  { return TokenType.BAD_CHARACTER; }
}
