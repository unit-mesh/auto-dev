// This is a generated file. Not intended for manual editing.
package cc.unitmesh.devti.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static cc.unitmesh.devti.language.psi.DevInTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class DevInParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, null);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return DevInFile(builder_, level_ + 1);
  }

  /* ********************************************************** */
  // (used | code | TEXT_SEGMENT | NEWLINE)*
  static boolean DevInFile(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "DevInFile")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!DevInFile_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "DevInFile", pos_)) break;
    }
    return true;
  }

  // used | code | TEXT_SEGMENT | NEWLINE
  private static boolean DevInFile_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "DevInFile_0")) return false;
    boolean result_;
    result_ = used(builder_, level_ + 1);
    if (!result_) result_ = code(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, TEXT_SEGMENT);
    if (!result_) result_ = consumeToken(builder_, NEWLINE);
    return result_;
  }

  /* ********************************************************** */
  // CODE_BLOCK_START LANGUAGE_ID? NEWLINE? code_contents? CODE_BLOCK_END?
  public static boolean code(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code")) return false;
    if (!nextTokenIs(builder_, CODE_BLOCK_START)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, CODE_BLOCK_START);
    result_ = result_ && code_1(builder_, level_ + 1);
    result_ = result_ && code_2(builder_, level_ + 1);
    result_ = result_ && code_3(builder_, level_ + 1);
    result_ = result_ && code_4(builder_, level_ + 1);
    exit_section_(builder_, marker_, CODE, result_);
    return result_;
  }

  // LANGUAGE_ID?
  private static boolean code_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_1")) return false;
    consumeToken(builder_, LANGUAGE_ID);
    return true;
  }

  // NEWLINE?
  private static boolean code_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_2")) return false;
    consumeToken(builder_, NEWLINE);
    return true;
  }

  // code_contents?
  private static boolean code_3(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_3")) return false;
    code_contents(builder_, level_ + 1);
    return true;
  }

  // CODE_BLOCK_END?
  private static boolean code_4(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_4")) return false;
    consumeToken(builder_, CODE_BLOCK_END);
    return true;
  }

  /* ********************************************************** */
  // (NEWLINE | CODE_CONTENT)*
  public static boolean code_contents(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_contents")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, CODE_CONTENTS, "<code contents>");
    while (true) {
      int pos_ = current_position_(builder_);
      if (!code_contents_0(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "code_contents", pos_)) break;
    }
    exit_section_(builder_, level_, marker_, true, false, null);
    return true;
  }

  // NEWLINE | CODE_CONTENT
  private static boolean code_contents_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "code_contents_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, NEWLINE);
    if (!result_) result_ = consumeToken(builder_, CODE_CONTENT);
    return result_;
  }

  /* ********************************************************** */
  // AGENT_START AGENT_ID (COLON PROPERTY_VALUE?)?
  //     | COMMAND_START COMMAND_ID
  //     | VARIABLE_START VARIABLE_ID
  public static boolean used(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "used")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, USED, "<used>");
    result_ = used_0(builder_, level_ + 1);
    if (!result_) result_ = parseTokens(builder_, 0, COMMAND_START, COMMAND_ID);
    if (!result_) result_ = parseTokens(builder_, 0, VARIABLE_START, VARIABLE_ID);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // AGENT_START AGENT_ID (COLON PROPERTY_VALUE?)?
  private static boolean used_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "used_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, AGENT_START, AGENT_ID);
    result_ = result_ && used_0_2(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // (COLON PROPERTY_VALUE?)?
  private static boolean used_0_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "used_0_2")) return false;
    used_0_2_0(builder_, level_ + 1);
    return true;
  }

  // COLON PROPERTY_VALUE?
  private static boolean used_0_2_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "used_0_2_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, COLON);
    result_ = result_ && used_0_2_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // PROPERTY_VALUE?
  private static boolean used_0_2_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "used_0_2_0_1")) return false;
    consumeToken(builder_, PROPERTY_VALUE);
    return true;
  }

}
