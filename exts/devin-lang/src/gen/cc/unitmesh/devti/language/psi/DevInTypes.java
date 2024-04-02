// This is a generated file. Not intended for manual editing.
package cc.unitmesh.devti.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import cc.unitmesh.devti.language.lexer.DevInTokenType;
import cc.unitmesh.devti.language.psi.impl.*;

public interface DevInTypes {

  IElementType CODE = new DevInElementType("CODE");
  IElementType CODE_CONTENTS = new DevInElementType("CODE_CONTENTS");
  IElementType USED = new DevInElementType("USED");

  IElementType AGENT_ID = new DevInTokenType("AGENT_ID");
  IElementType AGENT_START = new DevInTokenType("AGENT_START");
  IElementType CODE_BLOCK = new DevInTokenType("CODE_BLOCK");
  IElementType CODE_BLOCK_END = new DevInTokenType("CODE_BLOCK_END");
  IElementType CODE_BLOCK_START = new DevInTokenType("CODE_BLOCK_START");
  IElementType CODE_CONTENT = new DevInTokenType("CODE_CONTENT");
  IElementType COLON = new DevInTokenType("COLON");
  IElementType COMMAND_ID = new DevInTokenType("COMMAND_ID");
  IElementType COMMAND_START = new DevInTokenType("COMMAND_START");
  IElementType IDENTIFIER = new DevInTokenType("IDENTIFIER");
  IElementType LANGUAGE_ID = new DevInTokenType("LANGUAGE_ID");
  IElementType NEWLINE = new DevInTokenType("NEWLINE");
  IElementType PROPERTY_VALUE = new DevInTokenType("PROPERTY_VALUE");
  IElementType TEXT_SEGMENT = new DevInTokenType("TEXT_SEGMENT");
  IElementType VARIABLE_ID = new DevInTokenType("VARIABLE_ID");
  IElementType VARIABLE_START = new DevInTokenType("VARIABLE_START");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == CODE) {
        return new DevInCodeImpl(node);
      }
      else if (type == CODE_CONTENTS) {
        return new DevInCodeContentsImpl(node);
      }
      else if (type == USED) {
        return new DevInUsedImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
