package forms;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import services.FragmentService;

import java.util.List;

public class FragmentForm extends AndroidComponentForm {

    private static final String FRAGMENT_HEADER_TITLE = "Fragment Class Name:";
    private static final String FRAGMENT_CONTENT_TITLE = "Fragment Methods to Override:";

    public FragmentForm(@NotNull DataContext context, @NotNull Project project) {
        super(context, project);
    }

    @Override
    protected String getHeaderTitle() {
        return FRAGMENT_HEADER_TITLE;
    }

    @Override
    protected String getContentTitle() {
        return FRAGMENT_CONTENT_TITLE;
    }

    @Override
    protected List<PsiMethod> provideComponentMethods() {
        return FragmentService.getInstance(project).provideParentComponentMethods();
    }

    @Override
    protected void onCreateComponent() {
        final FragmentService fragmentService = FragmentService.getInstance(project);
        fragmentService.createComponentClass(currentFile, componentNameField.getText(), getSelectedComponentMethods());
    }
}
