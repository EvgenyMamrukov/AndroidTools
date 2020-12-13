package services;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FragmentService extends AndroidService {

    private static final String FRAGMENT_TAG = "fragment";

    protected static final String[] FRAGMENT_METHOD_NAMES = {
            ON_CREATE_VIEW, ON_START, ON_RESUME,
            ON_SAVE_INSTANCE_STATE,
            ON_PAUSE, ON_STOP, ON_DESTROY_VIEW
    };

    private static FragmentService instance;

    private FragmentService(@NotNull Project project) {
        super(project, ANDROIDX_FRAGMENT);
    }

    public static FragmentService getInstance(@NotNull Project project) {
        if (instance == null) {
            instance = new FragmentService(project);
        }

        return instance;
    }

    @Override
    protected String[] provideComponentMethodNames() {
        return FRAGMENT_METHOD_NAMES;
    }

    @Override
    protected List<PsiMethod> createComponentMethods(
            @NotNull PsiClass componentClass, @NotNull List<PsiMethod> methods
    ) {
        final List<PsiMethod> generatedMethods = new ArrayList<>();
        for (PsiMethod method : methods) {
            switch (method.getName()) {
                case ON_CREATE_VIEW:
                    generatedMethods.add(overrideOnCreateViewMethod(method, componentClass.getName()));
                    break;
                case ON_START:
                case ON_RESUME:
                case ON_SAVE_INSTANCE_STATE:
                case ON_PAUSE:
                case ON_STOP:
                case ON_DESTROY_VIEW:
                    generatedMethods.add(psiUtils.overrideMethod(method));
            }
        }

        return generatedMethods;
    }

    private PsiMethod overrideOnCreateViewMethod(@NotNull PsiMethod componentMethod, @Nullable String fragmentName) {
        final PsiMethod onCreateViewMethod = psiUtils.overrideMethod(componentMethod, false);
        if(fragmentName != null) {
            createComponentLayout(fragmentName, FRAGMENT_TAG);
            final PsiParameter[] parameters = onCreateViewMethod.getParameterList().getParameters();
            final String inflaterName = parameters[0].getName();
            final String containerName = parameters[1].getName();
            final PsiCodeBlock codeBlock = onCreateViewMethod.getBody();
            if (codeBlock != null) {
                final String returnText = PsiKeyword.RETURN
                        + " "
                        + inflaterName
                        + ".inflate(R.layout."
                        + createComponentLayoutName(fragmentName, FRAGMENT_TAG)
                        + ", "
                        + containerName
                        + ", false);";
                final PsiStatement returnStatement = elementFactory.createStatementFromText(
                        returnText, onCreateViewMethod
                );
                codeBlock.add(returnStatement);
            }
        }

        return onCreateViewMethod;
    }
}
