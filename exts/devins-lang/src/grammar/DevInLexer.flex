package cc.unitmesh.devti.language.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import static cc.unitmesh.devti.language.psi.DevInTypes.*;
import com.intellij.psi.TokenType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
%s COMMENT_BLOCK
%s LINE_BLOCK

%s LANG_ID

IDENTIFIER=[a-zA-Z0-9][_\-a-zA-Z0-9]*

VARIABLE_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
AGENT_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
COMMAND_ID=[a-zA-Z0-9][_\-a-zA-Z0-9]*
LANGUAGE_ID=[a-zA-Z][_\-a-zA-Z0-9 .]*
SYSTEM_ID=[a-zA-Z][_\-a-zA-Z0-9]*
NUMBER=[0-9]+
TEXT_SEGMENT=[^$/@#\n]+

// READ LINE FORMAT: L2C2-L0C100 or L1-L1
LINE_INFO=L[0-9]+(C[0-9]+)?(-L[0-9]+(C[0-9]+)?)?
//LINE_INFO=[L][0-9]+[L][0-9]+
COMMAND_PROP=[^\ \t\r\n]*
CODE_CONTENT=[^\n]+
COMMENTS=\[ ([^\]]+)? \] [^\t\r\n]*
NEWLINE= \n | \r | \r\n

COLON=:
SHARP=#

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
            if (text.startsWith("[")) {
                yybegin(COMMENT_BLOCK);
                return comment();
            }

            yypushback(yylength());
            yybegin(YYUSED);

            return TEXT_SEGMENT;
        }
    }

    private IElementType comment() {
        String text = yytext().toString().trim();
        if (text.contains("[") && text.contains("]")) {
            return COMMENTS;
        } else {
            return TEXT_SEGMENT;
        }
    }

    private IElementType command_value() {
        String text = yytext().toString().trim();
        String [] split = text.split("#");

        if (split.length == 1) {
            return COMMAND_PROP;
        }

        // split by # if it is a line info
        String last = split[split.length - 1];
        Pattern compile = Pattern.compile("L\\d+(C\\d+)?(-L\\d+(C\\d+)?)?");
        Matcher matcher = compile.matcher(last);
        if (matcher.matches()) {
            // before # is command prop, after # is line info
            int number = last.length() + "#".length();
            if (number > 0) {
                yypushback(number);
                yybegin(LINE_BLOCK);
                return COMMAND_PROP;
            } else {
                return COMMAND_PROP;
            }
        }

        return COMMAND_PROP;
    }
%}

%%
<YYINITIAL> {
  {CODE_CONTENT}          { return content(); }
  {NEWLINE}               { return NEWLINE;  }
  "["                     { yypushback(yylength()); yybegin(COMMENT_BLOCK);  }
  [^]                     { yypushback(yylength()); return TEXT_SEGMENT; }
}

<COMMENT_BLOCK> {
  {COMMENTS}              { return comment(); }
  [^]                     { yypushback(yylength()); yybegin(YYINITIAL); return TEXT_SEGMENT; }
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
  {COMMAND_PROP}          { return command_value();  }
  " "                     { yypushback(1); yybegin(YYINITIAL); }
  [^]                     { yypushback(1); yybegin(YYINITIAL); }
}

<LINE_BLOCK> {
  {LINE_INFO}             { return LINE_INFO; }
  {SHARP}                 { return SHARP; }
  [^]                     { yypushback(yylength()); yybegin(COMMAND_VALUE_BLOCK); }
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
  {SYSTEM_ID}          { return SYSTEM_ID; }
  {COLON}              { return COLON; }
  {NUMBER}             { return NUMBER; }
  [^]                  { yybegin(YYINITIAL); yypushback(yylength()); }
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
