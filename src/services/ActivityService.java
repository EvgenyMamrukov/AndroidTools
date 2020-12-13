package services;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ActivityService extends AndroidService {

    private static final String ACTIVITY_TAG = "activity";
    private static final String ANDROID_MANIFEST = "AndroidManifest.xml";
    private static final String APPLICATION_TAG = "application";
    private static final String NAME_ATTRIBUTE = "android:name";
    private static final String SET_CONTENT_VIEW = "setContentView";

    protected static final String[] ACTIVITY_METHOD_NAMES = {
            ON_CREATE, ON_START, ON_RESUME,
            ON_SAVE_INSTANCE_STATE,
            ON_PAUSE, ON_STOP, ON_DESTROY
    };

    private static ActivityService instance;

    private ActivityService(@NotNull Project project) {
        super(project, ANDROIDX_ACTIVITY);
    }

    public static ActivityService getInstance(@NotNull Project project) {
        if (instance == null) {
            instance = new ActivityService(project);
        }

        return instance;
    }

    @Override
    protected String[] provideComponentMethodNames() {
        return ACTIVITY_METHOD_NAMES;
    }

    @Override
    protected List<PsiMethod> createComponentMethods(
            @NotNull PsiClass componentClass, @NotNull List<PsiMethod> methods
    ) {
        final List<PsiMethod> generatedMethods = new ArrayList<>();
        for (PsiMethod method : methods) {
            switch (method.getName()) {
                case ON_CREATE:
                    generatedMethods.add(overrideOnCreateMethod(method, componentClass.getName()));
                    break;
                case ON_START:
                case ON_RESUME:
                case ON_SAVE_INSTANCE_STATE:
                case ON_PAUSE:
                case ON_STOP:
                case ON_DESTROY:
                    generatedMethods.add(psiUtils.overrideMethod(method));
            }
        }

        return generatedMethods;
    }

    @Override
    protected void onCreateComponent(@Nonnull PsiClass componentClass) {
        register(componentClass.getName());
    }

    private PsiMethod overrideOnCreateMethod(@NotNull PsiMethod componentMethod, @Nullable String activityName) {
        final PsiMethod onCreateMethod = psiUtils.overrideMethod(componentMethod);
        if (activityName != null) {
            createComponentLayout(activityName, ACTIVITY_TAG);
            final String activityLayoutName = createComponentLayoutName(activityName, ACTIVITY_TAG);
            final PsiCodeBlock codeBlock = onCreateMethod.getBody();
            if (codeBlock != null) {
                final String setContentViewText = SET_CONTENT_VIEW + "(" + "R.layout." + activityLayoutName + ");";
                final PsiStatement callSuperStatement = elementFactory.createStatementFromText(
                        setContentViewText, onCreateMethod
                );
                codeBlock.add(callSuperStatement);
            }
        }

        return onCreateMethod;
    }

    private void register(@Nullable String name) {
        final PsiFile[] files = FilenameIndex.getFilesByName(
                project, ANDROID_MANIFEST, GlobalSearchScope.projectScope(project)
        );
        if (files.length > 0 && files[0] instanceof XmlFile) {
            final XmlFile androidManifest = (XmlFile) files[0];
            final XmlTag manifestTag = androidManifest.getRootTag();
            if (manifestTag != null) {
                final XmlTag applicationTag = androidManifest.getRootTag().findFirstSubTag(APPLICATION_TAG);
                if (applicationTag != null && name != null) {
                    XmlTag activityTag = applicationTag.createChildTag(ACTIVITY_TAG, null, null, false);
                    activityTag.setAttribute(NAME_ATTRIBUTE, "." + name);
                    applicationTag.addSubTag(activityTag, false);
                }
            }
        }
    }
}
