package com.navitel.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import forms.AndroidComponentForm;
import forms.DialogFragmentForm;
import org.jetbrains.annotations.NotNull;

public class CreateDialogFragmentAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        if (project != null) {
            final AndroidComponentForm newFragmentDialogForm = new DialogFragmentForm(
                    anActionEvent.getDataContext(), project
            );
            newFragmentDialogForm.show();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        anActionEvent.getPresentation().setEnabled(anActionEvent.getProject() != null);
    }
}
