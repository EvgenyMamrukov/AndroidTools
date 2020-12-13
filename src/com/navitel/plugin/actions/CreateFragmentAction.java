package com.navitel.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import forms.AndroidComponentForm;
import forms.FragmentForm;
import org.jetbrains.annotations.NotNull;

public class CreateFragmentAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        final Project project = anActionEvent.getProject();
        if (project != null) {
            final AndroidComponentForm newFragmentForm = new FragmentForm(anActionEvent.getDataContext(), project);
            newFragmentForm.show();
        }
    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        anActionEvent.getPresentation().setEnabled(anActionEvent.getProject() != null);
    }
}
