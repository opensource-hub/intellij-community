/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring;

import com.intellij.JavaTestUtil;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.ide.DataManager;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.JavaNameSuggestionProvider;
import com.intellij.refactoring.rename.inplace.MemberInplaceRenameHandler;
import com.intellij.testFramework.LightCodeInsightTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class RenameMembersInplaceTest extends LightCodeInsightTestCase {
  private static final String BASE_PATH = "/refactoring/renameInplace/";

  @NotNull
  @Override
  protected String getTestDataPath() {
    return JavaTestUtil.getJavaTestDataPath();
  }

  public void testInnerClass() throws Exception {
    doTestInplaceRename("NEW_NAME");
  }
  
  public void testClassWithConstructorReferenceInside() throws Exception {
    doTestInplaceRename("NewName");
  }
  
  public void testIncomplete() throws Exception {
    doTestInplaceRename("Klazz");
  }
  
  public void testConstructor() throws Exception {
    doTestInplaceRename("Bar");
  }

  public void testSuperMethod() throws Exception {
    doTestInplaceRename("xxx");
  }
  
  public void testSuperMethodAnonymousInheritor() throws Exception {
    doTestInplaceRename("xxx");
  }

  public void testMultipleConstructors() throws Exception {
    doTestInplaceRename("Bar");
  }

  public void testClassWithMultipleConstructors() throws Exception {
    doTestInplaceRename("Bar");
  }
  
  public void testMethodWithJavadocRef() throws Exception {
    doTestInplaceRename("bar");
  }
  
  public void testEnumConstructor() throws Exception {
    doTestInplaceRename("Bar");
  }

  public void testMethodWithMethodRef() throws Exception {
    doTestInplaceRename("bar");
  }

  public void testRenameFieldInIncompleteStatement() throws Exception {
    doTestInplaceRename("bar");
  }

  public void testSameNamedMethodsInOneFile() throws Exception {
    configureByFile(BASE_PATH + "/" + getTestName(false) + ".java");

    final PsiElement element = TargetElementUtil.findTargetElement(myEditor, TargetElementUtil.getInstance().getAllAccepted());
    assertNotNull(element);

    Editor editor = getEditor();
    Project project = editor.getProject();
    TemplateManagerImpl.setTemplateTesting(getProject(), getTestRootDisposable());
    new MemberInplaceRenameHandler().doRename(element, editor, DataManager.getInstance().getDataContext(editor.getComponent()));
    TemplateState state = TemplateManagerImpl.getTemplateState(editor);
    assert state != null;
    assertEquals(2, state.getSegmentsCount());
    final TextRange range = state.getCurrentVariableRange();
    assert range != null;
    final Editor finalEditor = editor;
    new WriteCommandAction.Simple(project) {
      @Override
      protected void run() throws Throwable {
        finalEditor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), "newDoSomething");
      }
    }.execute().throwException();

    state = TemplateManagerImpl.getTemplateState(editor);
    assert state != null;
    state.gotoEnd(false);

    checkResultByFile(BASE_PATH + getTestName(false) + "_after.java");
  }

  public void testNameSuggestion() throws Exception {
    configureByFile(BASE_PATH + "/" + getTestName(false) + ".java");

    final PsiElement element = TargetElementUtil.findTargetElement(myEditor, TargetElementUtil.getInstance().getAllAccepted());
    assertNotNull(element);

    final Set<String> result = new LinkedHashSet<>();
    new JavaNameSuggestionProvider().getSuggestedNames(element, getFile(), result);

    CodeInsightTestUtil.doInlineRename(new MemberInplaceRenameHandler(), result.iterator().next(), getEditor(), element);

    checkResultByFile(BASE_PATH + getTestName(false) + "_after.java");
  }

  public void testConflictingMethodName() throws Exception {
    try {
      doTestInplaceRename("bar");
    }
    catch (BaseRefactoringProcessor.ConflictsInTestsException e) {
      assertEquals("Method bar() is already defined in the class <b><code>Foo</code></b>", e.getMessage());
      checkResultByFile(BASE_PATH + getTestName(false) + "_after.java");
      return;
    }
    fail("Conflict was not detected");
  }

  private void doTestInplaceRename(final String newName) throws Exception {
    configureByFile(BASE_PATH + "/" + getTestName(false) + ".java");

    final PsiElement element = TargetElementUtil.findTargetElement(myEditor, TargetElementUtil.getInstance().getAllAccepted());
    assertNotNull(element);

    CodeInsightTestUtil.doInlineRename(new MemberInplaceRenameHandler(), newName, getEditor(), element);

    checkResultByFile(BASE_PATH + getTestName(false) + "_after.java");
  }
}
