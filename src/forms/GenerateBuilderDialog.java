package forms;

import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class GenerateBuilderDialog extends DialogWrapper {

    private final LabeledComponent<JPanel> builderComponent;
    private final CollectionListModel<PsiField> builderFields;

    public GenerateBuilderDialog(PsiClass psiClass) {
        super(psiClass.getProject());

        setTitle("Select Builder Fields");
        builderFields = new CollectionListModel<>(psiClass.getAllFields());
        JBList<PsiField> items = new JBList<>(builderFields);
        items.setCellRenderer(new DefaultPsiElementCellRenderer());
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(items);
        JPanel panel = toolbarDecorator.createPanel();
        builderComponent = LabeledComponent.create(panel, "Fields to include in builder");

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return builderComponent;
    }

    public List<PsiField> getFields() {
        return builderFields.getItems();
    }
}
