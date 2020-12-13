package forms;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import renderers.PsiMethodCellListRenderer;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class AndroidComponentForm extends DialogWrapper {

    protected static final String HEADER_TITLE = "Android Component Name:";
    protected static final String CONTENT_TITLE = "Android Component Methods to Override:";

    protected final VirtualFile currentFile;
    protected final Project project;
    protected final JBTextField componentNameField;
    private final CollectionListModel<PsiMethod> componentListItems;

    public AndroidComponentForm(@NotNull DataContext context, @NotNull Project project) {
        super(true);
        this.currentFile = context.getData(CommonDataKeys.VIRTUAL_FILE);;
        this.project = project;
        componentListItems = new CollectionListModel<>(provideComponentMethods());
        componentNameField = new JBTextField();

        setOKActionEnabled(false);
        setUndecorated(true);
        init();
    }

    @Nullable
    @Override
    protected JComponent createNorthPanel() {
        final GridLayout contentLayout = new GridLayout(2, 1, 0, 0);
        final JLabel title = new JLabel(getHeaderTitle());
        final JPanel dialogPanel = new JPanel(contentLayout);
        dialogPanel.add(title);
        dialogPanel.add(componentNameField);
        componentNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                setOKActionEnabled(!componentNameField.getText().isEmpty());
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                setOKActionEnabled(!componentNameField.getText().isEmpty());
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                setOKActionEnabled(!componentNameField.getText().isEmpty());
            }
        });

        return dialogPanel;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        final JBList<PsiMethod> items = new JBList<>(componentListItems);
        final ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(items);
        final JPanel panel = toolbarDecorator.createPanel();
        final LabeledComponent<JPanel> component = LabeledComponent.create(panel, getContentTitle());
        final Dimension componentDimension = component.getPreferredSize();
        component.setPreferredSize(new Dimension(400, 220));
        items.setCellRenderer(new PsiMethodCellListRenderer(project));
        component.setBorder(JBUI.Borders.emptyTop(16));
        component.getLabel().setBorder(JBUI.Borders.emptyBottom(4));

        return component;
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        onCreateComponent();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return componentNameField;
    }

    protected String getHeaderTitle() {
        return HEADER_TITLE;
    }

    protected String getContentTitle() {
        return CONTENT_TITLE;
    }

    protected List<PsiMethod> getSelectedComponentMethods() {
        return componentListItems.getItems();
    }

    protected List<PsiMethod> provideComponentMethods() {
        return Collections.emptyList();
    }

    protected void onCreateComponent() {

    }
}
