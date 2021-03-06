/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.xml.breadcrumbs;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

final class ToggleBreadcrumbsAction extends ToggleAction implements DumbAware {
  private static final Key<Boolean> FORCED_BREADCRUMBS = new Key<>("FORCED_BREADCRUMBS");

  @Override
  public boolean isSelected(AnActionEvent event) {
    Editor editor = ToggleBreadcrumbsSettingsAction.findEditor(event);
    if (editor == null) return false;

    Boolean shown = getForcedShown(editor);
    if (shown != null) return shown;

    return ToggleBreadcrumbsSettingsAction.isSelected(editor);
  }

  @Override
  public void setSelected(AnActionEvent event, boolean selected) {
    Editor editor = ToggleBreadcrumbsSettingsAction.findEditor(event);
    if (editor != null) {
      editor.putUserData(FORCED_BREADCRUMBS, selected);
      UISettings.getInstance().fireUISettingsChanged();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    super.update(event);

    Editor editor = ToggleBreadcrumbsSettingsAction.findEditor(event);
    event.getPresentation().setEnabled(editor != null);
  }

  static Boolean getForcedShown(@NotNull Editor editor) {
    return editor.getUserData(FORCED_BREADCRUMBS);
  }
}
