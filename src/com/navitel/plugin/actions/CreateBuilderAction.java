package com.navitel.plugin.actions;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.PsiTreeUtil;
import forms.GenerateBuilderDialog;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateBuilderAction extends AnAction {

    private static final String OPERATOR_THIS = "this";
    private static final String OPERATOR_EQUALS = "=";
    private static final String BUILDER = "Builder";
    private static final String SETTER_NAME_PREFIX = "set";
    private static final String BUILD_NAME_PREFIX = "build";
    private static final String NEW = "new";
    private static final String RETURN = "return";
    private static final String BUILDER_ALREADY_EXISTS = "Builder class already exists";

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        PsiClass psiClass = providePsiClass(anActionEvent);
        if (psiClass == null) {
            return;
        }

        GenerateBuilderDialog dialog = new GenerateBuilderDialog(psiClass);
        dialog.show();
        if (dialog.isOK()) {
            final List<PsiField> dialogFields = dialog.getFields();
            Editor editor = anActionEvent.getData(LangDataKeys.EDITOR);
            generateConstructor(psiClass, dialogFields);
            generateBuilderClass(psiClass, dialogFields, editor);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent anActionEvent) {
        final PsiClass psiClass = providePsiClass(anActionEvent);
        anActionEvent.getPresentation().setEnabled(psiClass != null);
    }

    private PsiClass providePsiClass(@NotNull AnActionEvent anActionEvent) {
        PsiFile psiFile = anActionEvent.getData(LangDataKeys.PSI_FILE);
        Editor editor = anActionEvent.getData(LangDataKeys.EDITOR);
        if (psiFile == null || editor == null) {
            anActionEvent.getPresentation().setEnabled(false);
            return null;
        }
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        return PsiTreeUtil.getParentOfType(element, PsiClass.class);
    }

    private void generateConstructor(PsiClass psiClass, List<PsiField> psiFields) {
        new WriteCommandAction.Simple(psiClass.getProject(), psiClass.getContainingFile()) {
            @Override
            protected void run() throws Throwable {
                final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(getProject());

                if (psiClass.getName() == null) {
                    return;
                }

                final PsiMethod constructor = elementFactory.createConstructor(psiClass.getName(), psiClass);
                final StringBuilder bodyText = new StringBuilder();

                constructor.getModifierList().setModifierProperty(PsiModifier.PRIVATE, true);
                for (PsiField psiField : psiFields) {
                    final String fieldName = psiField.getName();
                    PsiParameter param = elementFactory.createParameter(psiField.getName(), psiField.getType());
                    constructor.getParameterList().add(param);
                    bodyText.append(OPERATOR_THIS + ".")
                            .append(fieldName)
                            .append(OPERATOR_EQUALS)
                            .append(fieldName)
                            .append(";");
                    PsiStatement body = elementFactory.createStatementFromText(bodyText.toString(), psiClass);
                    final PsiCodeBlock bodyCodeBlock = constructor.getBody();
                    if (bodyCodeBlock != null) {
                        bodyCodeBlock.add(body);
                    }
                    bodyText.setLength(0);
                }

                addMethod(psiClass, constructor);
            }
        }.execute();
    }

    private void generateBuilderClass(PsiClass targetClass, List<PsiField> targetClassFields, Editor editor) {
        new WriteCommandAction.Simple(targetClass.getProject(), targetClass.getContainingFile()) {

            @Override
            protected void run() throws Throwable {
                final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(getProject());
                final PsiClass builderClass = elementFactory.createClass(BUILDER);
                final PsiModifierList builderClassModifiers = builderClass.getModifierList();
                final StringBuilder builderMethodReturn = new StringBuilder(RETURN)
                        .append(" ")
                        .append(NEW + " ")
                        .append(targetClass.getName())
                        .append("(");

                if (builderClassModifiers != null && builderClass.getModifierList() != null) {
                    builderClass.getModifierList().setModifierProperty(PsiModifier.PUBLIC, true);
                    builderClass.getModifierList().setModifierProperty(PsiModifier.STATIC, true);
                }

                for (PsiField psiField : targetClassFields) {
                    final PsiModifierList builderFieldModifiers = psiField.getModifierList();
                    if (builderFieldModifiers != null) {
                        builderFieldModifiers.setModifierProperty(PsiModifier.PRIVATE, true);
                        psiField.setInitializer(null);
                        builderClass.add(psiField);
                    }

                    final String fieldName = psiField.getName();
                    final PsiType returnType = elementFactory.createType(builderClass);
                    final PsiParameter parameter = elementFactory.createParameter(fieldName, psiField.getType());
                    final String name = SETTER_NAME_PREFIX + capitalize(fieldName);
                    final PsiMethod builderSetter = elementFactory.createMethod(name, returnType, parameter);
                    builderSetter.getParameterList().add(parameter);
                    final StringBuilder body = new StringBuilder();
                    body.append(OPERATOR_THIS + ".")
                            .append(fieldName)
                            .append(OPERATOR_EQUALS)
                            .append(fieldName)
                            .append(";");
                    PsiStatement methodBodyStatement = elementFactory.createStatementFromText(
                            body.toString(), targetClass
                    );
                    final String returnText = RETURN + " " + OPERATOR_THIS + ";";
                    PsiStatement returnStatement = elementFactory.createStatementFromText(returnText, targetClass);

                    final PsiCodeBlock bodyCodeBlock = builderSetter.getBody();
                    if (bodyCodeBlock != null) {
                        bodyCodeBlock.add(methodBodyStatement);
                        bodyCodeBlock.add(returnStatement);
                    }
                    builderClass.add(builderSetter);

                    builderMethodReturn.append(psiField.getName()).append(",");
                }

                builderMethodReturn.setLength(builderMethodReturn.length() - 1);
                builderMethodReturn.append(");");
                final PsiType returnType = elementFactory.createType(targetClass);
                final PsiMethod builderMethod = elementFactory.createMethod(BUILD_NAME_PREFIX, returnType);
                final PsiStatement returnStatement = elementFactory.createStatementFromText(
                        builderMethodReturn.toString(), targetClass
                );
                final PsiCodeBlock bodyCodeBlock = builderMethod.getBody();
                if (bodyCodeBlock != null) {
                    bodyCodeBlock.add(returnStatement);
                }
                builderClass.add(builderMethod);

                addBuilderClass(editor, targetClass, builderClass);
            }
        }.execute();
    }

    private void addMethod(PsiClass parentClass, PsiMethod targetMethod) {
        final PsiMethod[] methods = parentClass.findMethodsByName(targetMethod.getName(), true);

        for (PsiMethod method : methods) {
            MethodSignature methodSignature = targetMethod.getSignature(PsiSubstitutor.UNKNOWN);
            MethodSignature creatingMethodSignature = method.getSignature(PsiSubstitutor.UNKNOWN);
            if (creatingMethodSignature.equals(methodSignature)) {
                return;
            }
        }

        parentClass.add(targetMethod);
    }

    private void addBuilderClass(Editor editor, PsiClass targetClass, PsiClass builderClass) {
        final PsiClass existingBuilderClass = targetClass.findInnerClassByName(builderClass.getName(), true);
        if (existingBuilderClass != null) {
            editor.getCaretModel().moveToOffset(existingBuilderClass.getTextOffset());
            final int offset = existingBuilderClass.getTextOffset();
            HintManager.getInstance().showErrorHint(
                    editor,
                    BUILDER_ALREADY_EXISTS,
                    offset,
                    offset,
                    HintManager.ABOVE,
                    HintManager.HIDE_BY_ANY_KEY|HintManager.HIDE_BY_SCROLLING,
                    0
            );
            existingBuilderClass.replace(builderClass);
        } else {
            targetClass.add(builderClass);
        }
    }

    public String capitalize(String string) {
        if (string == null || string.isEmpty()) {
            return string;
        }

        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }
}
