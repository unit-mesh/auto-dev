package cc.unitmesh.language.lexer;

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

%s AGENT_BLOCK
%s VARIABLE_BLOCK
%s COMMAND_BLOCK
%s CODE_BLOCK
%s LANG_ID

IDENTIFIER=[a-zA-Z0-9][_\-a-zA-Z0-9]*

VARIABLE_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
AGENT_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
COMMAND_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
LANGUAGE_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*

TEXT_SEGMENT=[^$/@\n]+
CODE_CONTENT=[^\n]+
NEWLINE= \n | \r | \r\n

%{
    private boolean isCodeStart = false;
%}

%{
    private IElementType codeContent() {
        yybegin(YYINITIAL);

        // handle for end which is \n```
        String text = yytext().toString().trim();
        if ((text.equals("\n```") || text.equals("```")) && isCodeStart == true ) {
            isCodeStart = false;
            return CODE_BLOCK_END;
        }

        // new line
        if (text.equals("\n")) {
            return NEWLINE;
        }

        if (isCodeStart == false) {
            return TEXT_SEGMENT;
        }

        return CODE_CONTENT;
    }
%}

%%
<YYINITIAL> {
  "@"                  { if(!isCodeStart) { yybegin(AGENT_BLOCK); return AGENT_START; } else { yypushback(1); yybegin(CODE_BLOCK); }}
  "/"                  { if(!isCodeStart) { yybegin(COMMAND_BLOCK); return COMMAND_START; } else { yypushback(1); yybegin(CODE_BLOCK); }}
  "$"                  { if(!isCodeStart) { yybegin(VARIABLE_BLOCK); return VARIABLE_START; } else { yypushback(1); yybegin(CODE_BLOCK); }}

  "```" {IDENTIFIER}?  { yybegin(LANG_ID); if (isCodeStart == true) { isCodeStart = false; return CODE_BLOCK_END; } else { isCodeStart = true; }; yypushback(yylength()); }

  {TEXT_SEGMENT}       { if(isCodeStart) { return codeContent(); } else { return TEXT_SEGMENT; } }
  {NEWLINE}            { return NEWLINE;  }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<AGENT_BLOCK> {
  {IDENTIFIER}         { yybegin(YYINITIAL); return AGENT_ID; }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<COMMAND_BLOCK> {
  {COMMAND_ID}         { yybegin(YYINITIAL); return COMMAND_ID; }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<VARIABLE_BLOCK> {
  {VARIABLE_ID}        { yybegin(YYINITIAL); return VARIABLE_ID; }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<CODE_BLOCK> {
  {CODE_CONTENT}       { if(isCodeStart) { return codeContent(); } else { yybegin(YYINITIAL); yypushback(yylength()); } }
  {NEWLINE}            { return NEWLINE; }
  <<EOF>>              { isCodeStart = false; yybegin(YYINITIAL); yypushback(yylength()); }
}

<LANG_ID> {
   "```"             { return CODE_BLOCK_START; }
   {IDENTIFIER}      { yybegin(YYINITIAL); return LANGUAGE_ID;  }
   [^]               { yypushback(1); yybegin(CODE_BLOCK); }
}
