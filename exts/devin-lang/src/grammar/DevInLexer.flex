// Copyright 2000-2022 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cc.unitmesh.language;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import cc.unitmesh.language.psi.DevInTypes;
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

CRLF=\R
WHITE_SPACE=[\ \n\t\f]
// $ variable
STRING=\"([^\\\"\r\n]|\\[^\r\n])*\"?
IDENTIFIER=[_a-zA-Z][_a-zA-Z0-9]*

%state WAITING_VALUE

%%
<YYINITIAL> {
    {STRING}           { return DevInTypes.STRING; }
    {IDENTIFIER}       { return DevInTypes.IDENTIFIER; }
}

({CRLF}|{WHITE_SPACE})+                                     { yybegin(YYINITIAL); return TokenType.WHITE_SPACE; }

[^]                                                         { return TokenType.BAD_CHARACTER; }
