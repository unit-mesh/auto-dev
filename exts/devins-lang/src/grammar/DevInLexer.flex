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
%s SINGLE_COMMNET_BLOCK
%s COMMAND_VALUE_BLOCK

%s EXPR_BLOCK

%s CODE_BLOCK
%s CONTENT_COMMENT_BLOCK
%s LINE_BLOCK
%s FRONT_MATTER_BLOCK
%s FRONT_MATTER_VALUE_BLOCK
%s FRONT_MATTER_VAL_OBJECT
%s PATTERN_ACTION_BLOCK
%s CONDITION_EXPR_BLOCK
%s FUNCTION_DECL_BLOCK
%s EXT_FUNCTION_BLOCK

%s LANG_ID

SPACE                    = [ \t\n\x0B\f\r]+
IDENTIFIER               = [a-zA-Z0-9][_\-a-zA-Z0-9]*
FRONTMATTER_KEY          = [a-zA-Z0-9][_\-a-zA-Z0-9]*
DATE                     = [0-9]{4}-[0-9]{2}-[0-9]{2}
STRING                   = [a-zA-Z0-9][_\-a-zA-Z0-9]*
// in Intellij Platform, the language id enable whitespace
LANGUAGE_ID      = [a-zA-Z][_\-a-zA-Z0-9 .]*

EOL=\R
INDENT                   = [ \t][ \t]+
WHITE_SPACE              = [ \t]+
COMMENTS                 = "//"[^\r\n]*
CONTENT_COMMENTS         = \[ ([^\]]+)? \] [^\t\r\n]*
BLOCK_COMMENT            = [/][*][^*]*[*]+([^/*][^*]*[*]+)*[/]
EscapedChar              = "\\" [^\n]
RegexWord                = [^\r\n\\\"' \t$`()] | {EscapedChar}
REGEX                    = \/{RegexWord}+\/
PATTERN_EXPR             = \/{RegexWord}+\/

LPAREN                   = \(
RPAREN                   = \)
PIPE                     = \|

NUMBER                   = [0-9]+
BOOLEAN                  = true|false|TRUE|FALSE|"true"|"false"

TEXT_SEGMENT             = [^$/@#\n]+
DOUBLE_QUOTED_STRING     = \"([^\\\"\r\n]|\\[^\r\n])*\"?
SINGLE_QUOTED_STRING     = '([^\\'\r\n]|\\[^\r\n])*'?
QUOTE_STRING             = {DOUBLE_QUOTED_STRING}|{SINGLE_QUOTED_STRING}

// READ LINE FORMAT: L2C2-L0C100 or L1-L1
LINE_INFO                = L[0-9]+(C[0-9]+)?(-L[0-9]+(C[0-9]+)?)?
COMMAND_PROP             = [^\ \t\r\n]*
CODE_CONTENT             = [^\n]+
NEWLINE                  = \n | \r | \r\n

COLON                    =:
SHARP                    =#
LBRACKET                 =\[
RBRACKET                 =\]
COMMA                    =,
ACCESS                   =::
PROCESS                  =->

DEFAULT                  =default
CASE                     =case
ARROW                    ==>
WHEN                     =when
FUNCTIONS                =functions
CONDITION                =condition
IF                       =if
ELSE                     =else
ELSEIF                   =elseif
ENDIF                    =endif
END                      =end
AND                      =and

ON_STREAMING             =onStreaming
BEFORE_STREAMING         =beforeStreaming
ON_STREAMING_END         =onStreamingEnd
AFTER_STREAMING          =afterStreaming

%{
    private boolean isCodeStart = false;
    private boolean isInsideDevInTemplate = false;
    private boolean isInsideFunctionBlock = false;
    private boolean isInsideFrontMatter = false;
    private boolean hasFrontMatter = false;
    private boolean patternActionBraceStart = false;
    private int patternActionBraceLevel = 0;
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
                yybegin(CONTENT_COMMENT_BLOCK);
                return comment();
            }

            // get first char, if $, @, /, #, should be a YYUSED
            if (text.length() == 0) {
                return TEXT_SEGMENT;
            }

            char first  = text.charAt(0);
            if (first == '@') {
                yypushback(yylength() - 1);
                yybegin(AGENT_BLOCK);
                return AGENT_START;
            } else if (first == '/') {
                yypushback(yylength() - 1);
                yybegin(COMMAND_BLOCK);
                return COMMAND_START;
            } else if (first == '$') {
                yypushback(yylength() - 1);
                yybegin(VARIABLE_BLOCK);
                return VARIABLE_START;
            }

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


    /** @param offset offset from currently matched token start (could be negative) */
    private char getCharAtOffset(final int offset) {
      final int loc = getTokenStart() + offset;
      return 0 <= loc && loc < zzBuffer.length() ? zzBuffer.charAt(loc) : (char) -1;
    }

    private boolean isAfterEol() {
      final char prev = getCharAtOffset(-1);
      return prev == (char)-1 || prev == '\n';
    }
%}

%%
<YYINITIAL> {
  "---"  {
          if (isCodeStart) {
              return CODE_CONTENT;
          } else {
              isInsideFrontMatter = true;
              yybegin(FRONT_MATTER_BLOCK);
              return FRONTMATTER_START;
          }
      }

  {CODE_CONTENT}          { return content(); }
  {NEWLINE}               { return NEWLINE;  }

  {BLOCK_COMMENT}         { return BLOCK_COMMENT; }
  {COMMENTS}              { return COMMENTS; }
}

<FRONT_MATTER_BLOCK> {
  {WHEN}                  { return WHEN; }
  {ON_STREAMING}          { return ON_STREAMING; }
  {BEFORE_STREAMING}      { return BEFORE_STREAMING; }
  {ON_STREAMING_END}      { return ON_STREAMING_END; }
  {AFTER_STREAMING}       { return AFTER_STREAMING; }
  {FUNCTIONS}             { return FUNCTIONS; }

  {IDENTIFIER}            { return IDENTIFIER; }
  {PATTERN_EXPR}          { return PATTERN_EXPR; }
  ":"                     { yybegin(FRONT_MATTER_VALUE_BLOCK); return COLON; }
  "{"                     { patternActionBraceLevel++; yybegin(FUNCTION_DECL_BLOCK); return OPEN_BRACE; }

  {INDENT}                { yybegin(FRONT_MATTER_VAL_OBJECT); return INDENT; }
  {NEWLINE}               { return NEWLINE; }

  "---"                   { isInsideFrontMatter = false; hasFrontMatter = true; yybegin(YYINITIAL); return FRONTMATTER_END; }
  [^]                     { yypushback(yylength()); yybegin(YYINITIAL); }
}

<FRONT_MATTER_VAL_OBJECT> {
  {COMMENTS}              { return COMMENTS; }
  {NEWLINE}               { return NEWLINE; }
  {QUOTE_STRING}          { return QUOTE_STRING; }

  ":"                     { yybegin(FRONT_MATTER_VALUE_BLOCK); return COLON; }
  [^]                     { yypushback(yylength()); yybegin(FRONT_MATTER_BLOCK); }
}

<FRONT_MATTER_VALUE_BLOCK>  {
  {NUMBER}                { return NUMBER; }
  {DATE}                  { return DATE; }
  {BOOLEAN}               { return BOOLEAN; }
  {IDENTIFIER}            { return IDENTIFIER; }
  {QUOTE_STRING}          { return QUOTE_STRING; }
  {PATTERN_EXPR}          { yybegin(PATTERN_ACTION_BLOCK); return PATTERN_EXPR; }

  {ACCESS}                { return ACCESS; }
  {PROCESS}               { return PROCESS; }

  "["                     { return LBRACKET; }
  "]"                     { return RBRACKET; }
  ","                     { return COMMA; }
  "!"                     { return NOT; }
  "&&"                    { return ANDAND; }
  "||"                    { return OROR; }
  "."                     { return DOT; }
  "=="                    { return EQEQ; }
  "!="                    { return NEQ; }
  "<"                     { return LT; }
  "<="                    { return LTE; }
  ">"                     { return GT; }
  ">="                    { return GTE; }
  "$"                     { return VARIABLE_START; }
  "("                     { return LPAREN; }
  ")"                     { return RPAREN; }

  {COMMENTS}              { return COMMENTS; }
  {WHITE_SPACE}           { return TokenType.WHITE_SPACE; }
  [^]                     { yypushback(yylength()); yybegin(FRONT_MATTER_BLOCK); }
}

<PATTERN_ACTION_BLOCK> {
  "{"                    { patternActionBraceStart = true; patternActionBraceLevel++; return OPEN_BRACE; }
  "}"                    { patternActionBraceLevel--; return CLOSE_BRACE; }
  "|"                    { return PIPE; }
  ","                    { return COMMA; }
  "("                    { return LPAREN; }
  ")"                    { return RPAREN; }
  {INDENT}               {
          if (patternActionBraceStart && patternActionBraceLevel == 0) {
              patternActionBraceStart = false;
              yybegin(FRONT_MATTER_VAL_OBJECT);
              return INDENT;
          } else {
              return TokenType.WHITE_SPACE;
          }
      }
  {WHITE_SPACE}          { return TokenType.WHITE_SPACE; }
  {NEWLINE}              { return NEWLINE; }

  // keywords
  "case"                 { return CASE; }
  "default"              { return DEFAULT; }

  {IDENTIFIER}            {
          if (isInsideFunctionBlock) {
              yypushback(yylength()); yybegin(EXPR_BLOCK);
          } else {
              switch (yytext().toString().trim()) {
                  case "when": return WHEN;
                  case "onStreaming": return ON_STREAMING;
                  case "beforeStreaming": return BEFORE_STREAMING;
                  case "onStreamingEnd": return ON_STREAMING_END;
                  case "afterStreaming": return AFTER_STREAMING;
                  default: return IDENTIFIER;
              }
          }
      }

  {QUOTE_STRING}         { return QUOTE_STRING; }
  {PATTERN_EXPR}         { return PATTERN_EXPR; }
  "$"                    { return VARIABLE_START; }
  "=>"                   { return ARROW; }
  [^]                    { patternActionBraceStart = false; yypushback(yylength()); yybegin(FRONT_MATTER_VALUE_BLOCK); }
}

<CONTENT_COMMENT_BLOCK> {
  {CONTENT_COMMENTS}      { return comment(); }
  [^]                     { yypushback(yylength()); yybegin(YYINITIAL); return TEXT_SEGMENT; }
}

<FUNCTION_DECL_BLOCK> {
  "from"                  { return FROM; }
  "where"                 { return WHERE; }
  "select"                { return SELECT; }
  "condition"             { return CONDITION; }
  "case"                  { return CASE; }
  "default"               { return DEFAULT; }

  "{"                     { patternActionBraceLevel++; isInsideFunctionBlock = true; yybegin(EXPR_BLOCK); return OPEN_BRACE; }
  "}"                     { patternActionBraceLevel--; if (patternActionBraceLevel == 0) { isInsideFunctionBlock = false; } return CLOSE_BRACE; }

  {NUMBER}                { return NUMBER; }
  {IDENTIFIER}            {
          if (isInsideFunctionBlock) {
              yypushback(yylength()); yybegin(EXPR_BLOCK);
          } else {
              switch (yytext().toString().trim()) {
                  case "when": return WHEN;
                  case "onStreaming": return ON_STREAMING;
                  case "beforeStreaming": return BEFORE_STREAMING;
                  case "onStreamingEnd": return ON_STREAMING_END;
                  case "afterStreaming": return AFTER_STREAMING;
                  default: return IDENTIFIER;
              }
          }
      }

  {DATE}                  { return DATE; }
  {BOOLEAN}               { return BOOLEAN; }
  {QUOTE_STRING}          { return QUOTE_STRING; }
  {PATTERN_EXPR}          { yybegin(PATTERN_ACTION_BLOCK); return PATTERN_EXPR; }

  "["                     { return LBRACKET; }
  "]"                     { return RBRACKET; }
  ","                     { return COMMA; }
  "!"                     { return NOT; }
  "&&"                    { return ANDAND; }
  "||"                    { return OROR; }
  "."                     { return DOT; }
  "=="                    { return EQEQ; }
  "!="                    { return NEQ; }
  "<"                     { return LT; }
  "<="                    { return LTE; }
  ">"                     { return GT; }
  ">="                    { return GTE; }
  "$"                     { return VARIABLE_START; }
  "("                     { return LPAREN; }
  ")"                     { return RPAREN; }
  "|"                     { return PIPE; }

  {COMMENTS}              { return COMMENTS; }
  {BLOCK_COMMENT}         { return BLOCK_COMMENT; }
  {COMMA}                 { return COMMA; }

  {NEWLINE}               { return NEWLINE; }
  {WHITE_SPACE}           {
          if (isInsideFunctionBlock && patternActionBraceLevel == 0) {
              isInsideFunctionBlock = false;
              System.out.println("PatternActionBraceLevel: " + patternActionBraceLevel);
              yybegin(FRONT_MATTER_VAL_OBJECT);
              return INDENT;
          } else {
              return TokenType.WHITE_SPACE;
          }
      }

  [^]                     {
          isInsideFunctionBlock = false;
          yypushback(yylength());
          yybegin(FRONT_MATTER_BLOCK);
      }
}

<YYUSED> {
  "@"                     { yybegin(AGENT_BLOCK);    return AGENT_START; }
  "//"  {TEXT_SEGMENT}    { yybegin(SINGLE_COMMNET_BLOCK); return COMMENTS; }
  "/"                     { yybegin(COMMAND_BLOCK);  return COMMAND_START; }
  "$"                     { yybegin(VARIABLE_BLOCK); return VARIABLE_START; }

  "```" {IDENTIFIER}?     {
          yybegin(LANG_ID);
          if (isCodeStart == true) {
              isCodeStart = false;
              return CODE_BLOCK_END;
          } else {
              isCodeStart = true;
          };
          yypushback(yylength());
      }

  {NEWLINE}               { return NEWLINE; }
  {TEXT_SEGMENT}          { return TEXT_SEGMENT; }
  {SHARP}                 { yybegin(EXPR_BLOCK); return SHARP; }

  [^]                     { return TokenType.BAD_CHARACTER; }
}

<COMMAND_BLOCK> {
  {IDENTIFIER}            { return IDENTIFIER; }
  {COLON}                 { yybegin(COMMAND_VALUE_BLOCK); return COLON; }
  [^]                     { yypushback(1); yybegin(YYINITIAL); }
}

<SINGLE_COMMNET_BLOCK> {
  {NEWLINE}               { return NEWLINE; }
  [^]                     {
          yypushback(yylength());
          if (isInsideFrontMatter) {
              yybegin(FRONT_MATTER_BLOCK);
          } else {
              yybegin(YYINITIAL);
          }
      }
}

<COMMAND_VALUE_BLOCK> {
  {COMMAND_PROP}          { return command_value();  }
  [^]                     { yypushback(yylength()); yybegin(YYINITIAL); }
}

<LINE_BLOCK> {
  {SHARP}                 { return SHARP; }
  {LINE_INFO}             { return LINE_INFO; }
  [^]                     { yypushback(yylength()); yybegin(COMMAND_VALUE_BLOCK); }
}

<AGENT_BLOCK> {
  {IDENTIFIER}           { yybegin(YYINITIAL); return IDENTIFIER; }
  {QUOTE_STRING}         { yybegin(YYINITIAL); return QUOTE_STRING; }
  [^]                    { yypushback(yylength()); yybegin(YYINITIAL); }
}

<VARIABLE_BLOCK> {
  {IDENTIFIER}         { return IDENTIFIER; }
  "{"                  { return OPEN_BRACE; }
  "}"                  { return CLOSE_BRACE; }
  "."                  { return DOT; }
  "("                  { return LPAREN; }
  ")"                  { return RPAREN; }

  {COMMENTS}           { return COMMENTS; }
  [^]                  { yypushback(yylength()); yybegin(YYINITIAL); }
}

<EXPR_BLOCK> {
  {IF}                 { return IF; }
  {ELSE}               { return ELSE; }
  {ELSEIF}             { return ELSEIF; }
  {ENDIF}              { return ENDIF; }
  {END}                { return END; }
  {AND}                { return AND; }
  "("                  { return LPAREN; }
  ")"                  { return RPAREN; }
  "<"                  { return LT; }
  "["                  { return LBRACKET; }
  "]"                  { return RBRACKET; }
  ","                  { return COMMA; }
  "!"                  { return NOT; }
  "&&"                 { return ANDAND; }
  "||"                 { return OROR; }
  "."                  { return DOT; }
  "=="                 { return EQEQ; }
  "!="                 { return NEQ; }
  "<"                  { return LT; }
  "<="                 { return LTE; }
  ">"                  { return GT; }
  ">="                 { return GTE; }
  "$"                  { return VARIABLE_START; }
  "{"                  { return OPEN_BRACE; }
  "}"                  { return CLOSE_BRACE; }

  {COMMA}              { return COMMA; }
  {NUMBER}             { return NUMBER; }
  {IDENTIFIER}         { return IDENTIFIER; }
  {QUOTE_STRING}       { return QUOTE_STRING; }
  {WHITE_SPACE}        { return TokenType.WHITE_SPACE; }

  // FOR Markdown Header
  "#"                  { yybegin(YYUSED); return SHARP; }
  [^]                  { yypushback(yylength()); if (isInsideDevInTemplate) { yybegin(CODE_BLOCK); }  else if (isInsideFunctionBlock) { yybegin(FUNCTION_DECL_BLOCK);} else { yybegin(YYINITIAL); } }
}

<CODE_BLOCK> {
  {CODE_CONTENT}       { if(isCodeStart) { return codeContent(); } else { yybegin(YYINITIAL); yypushback(yylength()); } }
  {NEWLINE}            { return NEWLINE; }
  <<EOF>>              { isCodeStart = false; isInsideDevInTemplate = false; yybegin(YYINITIAL); yypushback(yylength()); }
}

<LANG_ID> {
   "```"             { return CODE_BLOCK_START; }
//   {LANGUAGE_IDENTIFIER}     { return LANGUAGE_IDENTIFIER;  }
   {LANGUAGE_ID}     { return LANGUAGE_ID;  }
   "$"               { isInsideDevInTemplate = true; yybegin(EXPR_BLOCK); return VARIABLE_START; }
   [^]               { yypushback(yylength()); yybegin(CODE_BLOCK); }
}
