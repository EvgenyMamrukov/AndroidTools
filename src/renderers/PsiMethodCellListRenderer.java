package renderers;

import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.PsiUtils;

import javax.swing.*;

public class PsiMethodCellListRenderer extends PsiElementListCellRenderer<PsiMethod> {

    private final PsiUtils psiUtils;

    public PsiMethodCellListRenderer(@NotNull Project project) {
        psiUtils = PsiUtils.getInstance(project);
    }

    protected int getIconFlags() {
        return 1;
    }

    public String getElementText(PsiMethod method) {
        return psiUtils.createMethodDeclaration(method);
    }

    public String getContainerText(@Nullable PsiMethod method, String name) {
        if (method != null && method.getParent() instanceof PsiClass) {
            final PsiClass parentClass = (PsiClass) method.getParent();
            return parentClass.getName();
        }

        return "";
    }

    @Nullable
    protected DefaultListCellRenderer getRightCellRenderer(Object value) {
        return null;
    }
}
