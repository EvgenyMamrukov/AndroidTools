package forms;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import services.DialogFragmentService;

import java.util.List;

public class DialogFragmentForm extends AndroidComponentForm {

    private static final String DIALOG_FRAGMENT_HEADER_TITLE = "Dialog Fragment Class Name:";
    private static final String DIALOG_FRAGMENT_CONTENT_TITLE = " Dialog Fragment Methods to Override:";

    public DialogFragmentForm(@NotNull DataContext context, @NotNull Project project) {
        super(context, project);
    }

    @Override
    protected String getHeaderTitle() {
        return DIALOG_FRAGMENT_HEADER_TITLE;
    }

    @Override
    protected String getContentTitle() {
        return DIALOG_FRAGMENT_CONTENT_TITLE;
    }

    @Override
    protected List<PsiMethod> provideComponentMethods() {
        return DialogFragmentService.getInstance(project).provideParentComponentMethods();
    }

    @Override
    protected void onCreateComponent() {
        final DialogFragmentService dialogFragmentService = DialogFragmentService.getInstance(project);
        dialogFragmentService.createComponentClass(
                currentFile, componentNameField.getText(), getSelectedComponentMethods()
        );
    }
}
