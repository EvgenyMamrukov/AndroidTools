package forms;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import services.ActivityService;

import java.util.List;

public class ActivityForm extends AndroidComponentForm {

    private static final String ACTIVITY_HEADER_TITLE = "Activity Class Name:";
    private static final String ACTIVITY_CONTENT_TITLE = "Activity Methods to Override:";

    public ActivityForm(@NotNull DataContext context, @NotNull Project project) {
        super(context, project);
    }

    @Override
    protected String getHeaderTitle() {
        return ACTIVITY_HEADER_TITLE;
    }

    @Override
    protected String getContentTitle() {
        return ACTIVITY_CONTENT_TITLE;
    }

    @Override
    protected List<PsiMethod> provideComponentMethods() {
        final ActivityService activityService = ActivityService.getInstance(project);
        return activityService.provideParentComponentMethods();
    }

    @Override
    protected void onCreateComponent() {
        final ActivityService activityService = ActivityService.getInstance(project);
        activityService.createComponentClass(currentFile, componentNameField.getText(), getSelectedComponentMethods());
    }
}
