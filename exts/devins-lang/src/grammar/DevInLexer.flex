package cc.unitmesh.devti.language.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static cc.unitmesh.devti.language.psi.DevInTypes.*;
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

%s YYUSED
%s AGENT_BLOCK
%s VARIABLE_BLOCK

%s COMMAND_BLOCK
%s COMMAND_VALUE_BLOCK

%s SYSTEM_BLOCK

%s CODE_BLOCK
%s LANG_ID

IDENTIFIER=[a-zA-Z0-9][_\-a-zA-Z0-9]*

VARIABLE_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
AGENT_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
COMMAND_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
LANGUAGE_ID=[a-zA-Z0-9][_\-a-zA-Z0-9 .]*
SYSTEM_ID=[a-zA-Z0-9][_\-a-zA-Z0-9 .]*

TEXT_SEGMENT=[^$/@\n]+
COMMAND_PROP=[^:\ \t\r\n]*
CODE_CONTENT=[^\n]+
NEWLINE= \n | \r | \r\n

COLON=:

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

    private IElementType content() {
        String text = yytext().toString().trim();
        if (isCodeStart == true && text.equals("```")) {
            return codeContent();
        }

        if (isCodeStart == false && text.startsWith("```")) {
            isCodeStart = true;
            yypushback(yylength() - 3);
            yybegin(LANG_ID);

            return CODE_BLOCK_START;
        }

        if (isCodeStart) {
            return CODE_CONTENT;
        } else {
            yypushback(yylength());
            yybegin(YYUSED);

            return TEXT_SEGMENT;
        }
    }
%}

%%
<YYINITIAL> {
  {CODE_CONTENT}          { return content(); }
  {NEWLINE}               { return NEWLINE;  }
  [^]                     { return TokenType.BAD_CHARACTER; }
}

<YYUSED> {
  "@"                     { yybegin(AGENT_BLOCK);    return AGENT_START; }
  "/"                     { yybegin(COMMAND_BLOCK);  return COMMAND_START; }
  "$"                     { yybegin(VARIABLE_BLOCK); return VARIABLE_START; }
  "#"                     { yybegin(SYSTEM_BLOCK);   return SYSTEM_START; }

  "```" {IDENTIFIER}?     { yybegin(LANG_ID); if (isCodeStart == true) { isCodeStart = false; return CODE_BLOCK_END; } else { isCodeStart = true; }; yypushback(yylength()); }

  {NEWLINE}               { return NEWLINE; }
  {TEXT_SEGMENT}          { return TEXT_SEGMENT; }
  [^]                     { return TokenType.BAD_CHARACTER; }
}

<COMMAND_BLOCK> {
  {COMMAND_ID}            { return COMMAND_ID; }
  {COLON}                 { yybegin(COMMAND_VALUE_BLOCK); return COLON; }
  " "                     { yypushback(1); yybegin(YYINITIAL); }
  [^]                     { yypushback(1); yybegin(YYINITIAL); }
}

<COMMAND_VALUE_BLOCK> {
  {COMMAND_PROP}          { return COMMAND_PROP;  }
  " "                     { yypushback(1); yybegin(YYINITIAL); }
  [^]                     { yypushback(1); yybegin(YYINITIAL); }
}

<AGENT_BLOCK> {
  {AGENT_ID}           { yybegin(YYINITIAL); return AGENT_ID; }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<VARIABLE_BLOCK> {
  {VARIABLE_ID}        { yybegin(YYINITIAL); return VARIABLE_ID; }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<SYSTEM_BLOCK> {
  {SYSTEM_ID}        { yybegin(YYINITIAL); return SYSTEM_ID; }
  [^]                  { return TokenType.BAD_CHARACTER; }
}

<CODE_BLOCK> {
  {CODE_CONTENT}       { if(isCodeStart) { return codeContent(); } else { yybegin(YYINITIAL); yypushback(yylength()); } }
  {NEWLINE}            { return NEWLINE; }
  <<EOF>>              { isCodeStart = false; yybegin(YYINITIAL); yypushback(yylength()); }
}

<LANG_ID> {
   "```"             { return CODE_BLOCK_START; }
   {LANGUAGE_ID}     { return LANGUAGE_ID;  }
   [^]               { yypushback(1); yybegin(CODE_BLOCK); }
}
