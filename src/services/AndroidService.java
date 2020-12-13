package services;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.PsiUtils;
import utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AndroidService {

    protected static final String TEXT_UTILS = "android.text.TextUtils";
    protected static final String ANDROIDX_ALERT_DIALOG_BUILDER = "androidx.appcompat.app.AlertDialog.Builder";
    protected static final String ANDROIDX_ACTIVITY = "androidx.appcompat.app.AppCompatActivity";
    protected static final String ANDROIDX_DIALOG_FRAGMENT = "androidx.appcompat.app.AppCompatDialogFragment";
    protected static final String ANDROIDX_FRAGMENT = "androidx.fragment.app.Fragment";
    protected static final String ANDROIDX_FRAGMENT_MANAGER = "androidx.fragment.app.FragmentManager";
    protected static final String BUNDLE = "android.os.Bundle";
    protected static final String ANDROIDX_LAYOUT_RES = "androidx.annotation.LayoutRes";
    protected static final String ANDROIDX_NON_NULL = "androidx.annotation.NonNull";
    protected static final String ANDROIDX_NULLABLE = "androidx.annotation.Nullable";
    protected static final String ANDROIDX_STRING_RES = "androidx.annotation.StringRes";
    private static final String LAYOUT_TAG = "layout";
    private static final String XML_EXTENSION = ".xml";

    protected static final String ON_ATTACH = "onAttach";
    protected static final String ON_CREATE = "onCreate";
    protected static final String ON_CREATE_VIEW = "onCreateView";
    protected static final String ON_CREATE_DIALOG = "onCreateDialog";
    protected static final String ON_ACTIVITY_CREATED = "onActivityCreated";
    protected static final String ON_START = "onStart";
    protected static final String ON_RESUME = "onResume";
    protected static final String ON_SAVE_INSTANCE_STATE = "onSaveInstanceState";
    protected static final String ON_PAUSE = "onPause";
    protected static final String ON_STOP = "onStop";
    protected static final String ON_DESTROY_VIEW = "onDestroyView";
    protected static final String ON_DESTROY = "onDestroy";
    protected static final String ON_DISMISS = "onDismiss";
    protected static final String ON_DETACH = "onDetach";
    protected static final String[] COMPONENT_METHOD_NAMES = {
            ON_ATTACH, ON_CREATE, ON_CREATE_VIEW, ON_ACTIVITY_CREATED, ON_START, ON_RESUME,
            ON_SAVE_INSTANCE_STATE,
            ON_PAUSE, ON_STOP, ON_DESTROY_VIEW, ON_DETACH, ON_DESTROY
    };

    private static final String EMPTY_LAYOUT_TEMPLATE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<androidx.constraintlayout.widget.ConstraintLayout\n" +
            "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "    android:layout_width=\"match_parent\"\n" +
            "    android:layout_height=\"match_parent\">\n" +
            "</androidx.constraintlayout.widget.ConstraintLayout>";

    protected final Project project;
    protected final JavaCodeStyleManager javaCodeStyleManager;
    protected final JavaPsiFacade javaFacade;
    protected final GlobalSearchScope globalSearchScope;
    protected final PsiElementFactory elementFactory;
    protected final PsiUtils psiUtils;
    private final PsiDirectoryFactory directoryFactory;
    private final PsiFileFactory fileFactory;
    private final String parentComponentName;

    public AndroidService(@NotNull Project project, @Nullable String parentComponentName) {
        this.project = project;
        this.parentComponentName = parentComponentName;
        javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        javaFacade = JavaPsiFacade.getInstance(project);
        globalSearchScope = GlobalSearchScope.allScope(project);
        elementFactory = PsiElementFactory.getInstance(project);
        psiUtils = PsiUtils.getInstance(project);
        directoryFactory = PsiDirectoryFactory.getInstance(project);
        fileFactory = PsiFileFactory.getInstance(project);
    }

    public void createComponentClass(
            @Nullable VirtualFile currentFile, @NotNull String name, @NotNull List<PsiMethod> methods
    ) {
        if (currentFile != null && currentFile.exists()) {
            final VirtualFile classFile = currentFile.findChild(name + PsiUtils.JAVA_EXTENSION);
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            if (classFile != null && classFile.exists()) {
                fileEditorManager.openFile(classFile, true);
            } else {
                final PsiDirectory psiDirectory = directoryFactory.createDirectory(currentFile);
                try {
                    final PsiClass componentClass = JavaDirectoryService.getInstance().createClass(psiDirectory, name);
                    WriteCommandAction.runWriteCommandAction(project, () -> {
                        final PsiClass parentClass = javaFacade.findClass(parentComponentName, globalSearchScope);
                        if (parentClass != null) {
                            psiUtils.extendClass(componentClass, parentClass);
                        }
                        final List<PsiMethod> componentMethods = createComponentMethods(componentClass, methods);
                        for (PsiMethod method : componentMethods) {
                            componentClass.add(method);
                        }

                        onCreateComponent(componentClass);

                        fileEditorManager.openFile(componentClass.getContainingFile().getVirtualFile(), true);
                    });
                } catch (IncorrectOperationException exception) {
                    System.out.println(exception.getLocalizedMessage());
                }
            }
        }
    }

    public List<PsiMethod> provideParentComponentMethods() {
        return psiUtils.findMethods(parentComponentName, provideComponentMethodNames());
    }

    protected String[] provideComponentMethodNames() {
        return COMPONENT_METHOD_NAMES;
    }

    protected List<PsiMethod> createComponentMethods(
            @NotNull PsiClass componentClass, @NotNull List<PsiMethod> methods
    ) {

        return Collections.emptyList();
    }

    protected void onCreateComponent(@NotNull PsiClass componentClass) {

    }

    protected PsiStatement createPutToBundleStatement(
            @NotNull String bundle, @NotNull PsiType type, @NotNull String name, @NotNull String key
    ) {
        final PsiClass bundleClass = javaFacade.findClass(BUNDLE, globalSearchScope);
        final String typeName = StringUtils.capitalize(type.getPresentableText().toLowerCase());
        if (bundleClass != null) {
            final PsiMethod[] methods = bundleClass.findMethodsByName(PsiUtils.PUT + typeName, true);
            if (methods.length > 0) {
                final List<String> methodParams = new ArrayList<>();
                methodParams.add(key);
                methodParams.add(name);
                final String methodCallText = psiUtils.createMethodCall(
                        methods[0].getName(), methodParams, bundle, false
                );

                return elementFactory.createStatementFromText(methodCallText + ";", bundleClass);
            }
        }

        return null;
    }

    protected PsiStatement createGetFromBundleVariable(
            @NotNull String bundle,
            @NotNull PsiType type,
            @NotNull String variableName,
            @NotNull String key,
            @Nullable String defaultValue
    ) {
        final PsiClass bundleClass = javaFacade.findClass(BUNDLE, globalSearchScope);
        final String typeName = StringUtils.capitalize(type.getPresentableText().toLowerCase());
        if (bundleClass != null) {
            final PsiMethod[] methods = bundleClass.findMethodsByName(PsiUtils.GET + typeName, true);
            if (methods.length > 0) {
                final List<String> methodParams = new ArrayList<>();
                methodParams.add(key);
                if (defaultValue != null) {
                    methodParams.add(defaultValue);
                }
                final String methodCallText = psiUtils.createMethodCall(
                        methods[0].getName(), methodParams, bundle, false
                );

                final PsiExpression getFromBundle = elementFactory.createExpressionFromText(
                        methodCallText, null
                );
                return psiUtils.createVariable(getFromBundle, type, variableName);
            }
        }

        return null;
    }

    protected PsiExpression textIsEmptyCheck(@Nullable String text, boolean inverse) {
        final StringBuilder textIsEmpty = new StringBuilder();
        if (inverse) {
            textIsEmpty.append("!");
        }
        final List<String> methodParams = new ArrayList<>();
        methodParams.add(text);
        textIsEmpty.append(psiUtils.createMethodCall(PsiUtils.IS_EMPTY, methodParams, TEXT_UTILS, false));
        final PsiExpression textIsEmptyExpression = elementFactory.createExpressionFromText(
                textIsEmpty.toString(), null
        );
        javaCodeStyleManager.shortenClassReferences(textIsEmptyExpression);

        return textIsEmptyExpression;
    }

    protected String createComponentLayoutName(@NotNull String componentName, @NotNull String layoutPrefix) {

        return StringUtils.convertCamelToSnake(componentName, layoutPrefix, null, false);
    }

    protected void createComponentLayout(@NotNull String componentName, @NotNull String layoutPrefix) {
        final String activityLayoutName = createComponentLayoutName(componentName, layoutPrefix);
        if (activityLayoutName != null) {
            final VirtualFile[] sourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
            for (VirtualFile sourceRoot : sourceRoots) {
                final VirtualFile layoutDirectory =  sourceRoot.findFileByRelativePath(LAYOUT_TAG);
                if (layoutDirectory != null && layoutDirectory.exists()) {
                    final PsiDirectory psiDirectory = directoryFactory.createDirectory(layoutDirectory);
                    final PsiFile activityLayoutFile = fileFactory.createFileFromText(
                            activityLayoutName + XML_EXTENSION,
                            XmlFileType.INSTANCE,
                            EMPTY_LAYOUT_TEMPLATE
                    );
                    psiDirectory.add(activityLayoutFile);
                }
            }
        }
    }
}
