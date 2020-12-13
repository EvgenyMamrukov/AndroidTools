package com.navitel.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import forms.ActivityForm;
import forms.AndroidComponentForm;
import org.jetbrains.annotations.NotNull;

public class CreateActivityAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        if (project != null) {
            final AndroidComponentForm newActivityForm = new ActivityForm(anActionEvent.getDataContext(), project);
            newActivityForm.show();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        anActionEvent.getPresentation().setEnabled(anActionEvent.getProject() != null);
    }
}
