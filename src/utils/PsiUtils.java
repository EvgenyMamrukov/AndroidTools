package utils;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PsiUtils {

    public static final String JAVA_EXTENSION = ".java";
    public static final String OVERRIDE = "Override";
    public static final String PUT = "put";
    public static final String GET = "get";
    public static final String SET = "set";
    public static final String IS_EMPTY = "isEmpty";

    private final PsiElementFactory elementFactory;
    private final JavaCodeStyleManager javaCodeStyleManager;
    private final JavaPsiFacade javaFacade;
    private final GlobalSearchScope globalSearchScope;
    private final PsiManager psiManager;

    private static PsiUtils instance;

    private PsiUtils(@NotNull Project project) {
        elementFactory = PsiElementFactory.getInstance(project);
        javaCodeStyleManager = JavaCodeStyleManager.getInstance(project);
        javaFacade = JavaPsiFacade.getInstance(project);
        globalSearchScope = GlobalSearchScope.allScope(project);
        psiManager = PsiManager.getInstance(project);
    }

    public static PsiUtils getInstance(@NotNull Project project) {
        if (instance == null) {
            instance = new PsiUtils(project);
        }

        return instance;
    }

    public void extendClass(@NotNull PsiClass targetClass, @NotNull PsiClass parentClass) {
        if (targetClass.getExtendsList() != null) {
            final PsiJavaCodeReferenceElement parentComponentReference = elementFactory.createClassReferenceElement(
                    parentClass
            );
            targetClass.getExtendsList().add(parentComponentReference);
        }
    }

    public PsiMethod createConstructor(
            @NotNull PsiClass targetClass, @NotNull String accessModifier, @Nullable List<PsiParameter> parameters
    ) {
        final String name = targetClass.getName();
        final PsiMethod constructor;
        if (name != null) {
            constructor = elementFactory.createConstructor(name);
        } else {
            constructor = elementFactory.createConstructor();
        }
        constructor.getModifierList().setModifierProperty(accessModifier, true);
        if (parameters != null) {
            final PsiParameterList parameterList = constructor.getParameterList();
            for (PsiParameter parameter : parameters) {
                parameterList.add(parameter);
            }
        }

        return constructor;
    }

    public PsiMethod createMethod(
            @NotNull String name,
            @Nullable String accessModifier,
            @Nullable PsiType returnType,
            @Nullable PsiParameter[] parameters
    ) {
        final PsiMethod method = elementFactory.createMethod(name, returnType);

        if (accessModifier != null) {
            method.getModifierList().setModifierProperty(accessModifier, true);
        }

        if (parameters != null) {
            final PsiParameterList parameterList = method.getParameterList();
            for (PsiParameter parameter : parameters) {
                parameterList.add(parameter);
            }
        }

        return method;
    }

    public PsiMethod createInterfaceMethod(
            @NotNull String name, @Nullable PsiType returnType, @Nullable PsiParameter[] parameters
    ) {
        final PsiMethod interfaceMethod = createMethod(name, null, returnType, parameters);
        final PsiCodeBlock methodBody = interfaceMethod.getBody();
        if (methodBody != null) {
            methodBody.delete();
        }

        return interfaceMethod;
    }

    public PsiMethod overrideMethod(@NotNull PsiMethod method, boolean callSuper) {
        final PsiMethod overridedMethod = elementFactory.createMethod(method.getName(), method.getReturnType());
        addAnnotation(overridedMethod, OVERRIDE);

        final PsiParameter[] parameters = method.getParameterList().getParameters();
        for (PsiParameter parameter : parameters) {
            overridedMethod.getParameterList().add(parameter);
        }

        final PsiCodeBlock codeBlock = overridedMethod.getBody();
        if (callSuper && codeBlock != null) {
            final PsiType returnType = method.getReturnType();
            final StringBuilder callSuperText = new StringBuilder(
                    PsiType.VOID.equals(returnType) ? "" : PsiKeyword.RETURN + " "
            )
                    .append(PsiKeyword.SUPER + ".")
                    .append(overridedMethod.getName())
                    .append("(");
            for (int i = 0; i < parameters.length; i++) {
                callSuperText.append(parameters[i].getName());
                if (parameters.length > 1 && i < parameters.length - 1) {
                    callSuperText.append(", ");
                }
            }
            callSuperText.append(");");
            final PsiStatement callSuperStatement = elementFactory.createStatementFromText(
                    callSuperText.toString(), overridedMethod
            );
            codeBlock.add(callSuperStatement);
        }

        return overridedMethod;
    }

    public PsiMethod overrideMethod(@NotNull PsiMethod componentMethod) {
        return overrideMethod(componentMethod, true);
    }

    public void addMethodBody(@NotNull PsiMethod method, @NotNull List<PsiStatement> statements) {
        final PsiCodeBlock methodBody = method.getBody();
        if (methodBody != null) {
            for (PsiStatement statement : statements) {
                if (statement != null) {
                    methodBody.add(statement);
                }
            }
        }
    }

    public List<PsiMethod> findMethods(@NotNull String className, @NotNull String[] methodNames) {
        final List<PsiMethod> methods = new ArrayList<>();
        final PsiClass componentClass = javaFacade.findClass(className, globalSearchScope);

        if (componentClass != null) {
            for (String methodName : methodNames) {
                final PsiMethod[] foundedMethods = componentClass.findMethodsByName(methodName, true);
                if (foundedMethods.length > 0) {
                    final PsiMethod componentMethod = foundedMethods[0];
                    methods.add(componentMethod);
                }
            }
        }

        return methods;
    }

    public String createMethodDeclaration(@Nullable PsiMethod method) {
        if (method != null) {
            final StringBuilder methodText = new StringBuilder(method.getName()).append("(");
            final PsiParameter[] params = method.getParameterList().getParameters();
            for (int i = 0; i < params.length; i++) {
                methodText.append(params[i].getName())
                        .append(":")
                        .append(params[i].getType().getPresentableText());
                if (params.length > 1 && i < params.length - 1) {
                    methodText.append(", ");
                }
            }
            methodText.append(")");
            final PsiType returnType = method.getReturnType();
            if (returnType != null) {
                methodText.append(":").append(returnType.getPresentableText());
            }

            return methodText.toString();
        }

        return "";
    }

    public String createMethodCall(
            @NotNull String name, @Nullable List<String> params, @Nullable String objectName, boolean isConstructor
    ) {
        final StringBuilder methodCallText = new StringBuilder();
        if (objectName != null && !objectName.isEmpty()) {
            methodCallText.append(objectName).append(".");
        }
        if (isConstructor) {
            methodCallText.append(PsiKeyword.NEW).append(" ");
        }
        methodCallText.append(name).append("(");
        if (params != null) {
            final int paramsCount = params.size();
            for (int i = 0; i < paramsCount; i++) {
                methodCallText.append(params.get(i));
                if (paramsCount > 1 && i < paramsCount - 1) {
                    methodCallText.append(", ");
                }
            }
        }
        methodCallText.append(")");

        return methodCallText.toString();
    }

    public String createLambda(@Nullable List<String> params, @Nullable List<PsiStatement> lambdaStatements) {
        final StringBuilder lambdaTemplate = new StringBuilder("(");
        if (params != null) {
            final int paramsCount = params.size();
            for (int i = 0; i < paramsCount; i++) {
                lambdaTemplate.append(params.get(i));
                if (paramsCount > 1 && i < paramsCount - 1) {
                    lambdaTemplate.append(", ");
                }
            }
        }
        lambdaTemplate.append(")").append(" -> ").append("{}");
        final PsiStatement lambdaStatement = elementFactory.createStatementFromText(
                lambdaTemplate.toString(), null
        );
        final PsiLambdaExpression lambda = (PsiLambdaExpression) lambdaStatement.getFirstChild();

        if (lambdaStatements != null) {
            final PsiElement lambdaBody = lambda.getBody();
            if (lambdaBody != null) {
                for (PsiStatement statement : lambdaStatements) {
                    lambdaBody.add(statement);
                }
            }
        }

        return lambda.getText();
    }

    public PsiStatement createVariable(
            @Nullable PsiExpression initializer, @NotNull PsiType variableType, @NotNull String variableName
    ) {
        final PsiStatement createVariableStatement = elementFactory.createVariableDeclarationStatement(
                variableName, variableType,initializer
        );
        javaCodeStyleManager.shortenClassReferences(createVariableStatement);

        return createVariableStatement;
    }

    public PsiStatement createVariable(
            @NotNull PsiType variableType, @NotNull String variableName, @Nullable List<String> constructorParams
    ) {
        final String typeName = variableType.getCanonicalText();
        final String constructorCallText = createMethodCall(
                typeName, constructorParams, null, true
        );
        final PsiExpression variableExpression = elementFactory.createExpressionFromText(
                constructorCallText, null
        );
        final PsiStatement createVariableStatement = elementFactory.createVariableDeclarationStatement(
                variableName, variableType, variableExpression
        );
        javaCodeStyleManager.shortenClassReferences(createVariableStatement);

        return createVariableStatement;
    }

    public PsiField createField(
            @NotNull PsiType type,
            @Nullable List<String> modifiers,
            @NotNull String fieldName,
            @Nullable PsiExpression initializer
    ) {
        final PsiField field = elementFactory.createField(fieldName, type);
        if (modifiers != null) {
            final PsiModifierList modifierList = field.getModifierList();
            if (modifierList != null) {
                for (String modifier : modifiers) {
                    if (modifier != null) {
                        modifierList.setModifierProperty(modifier, true);
                    }
                }
            }

        }
        if (initializer != null) {
            field.setInitializer(initializer);
        }

        return field;
    }

    public PsiField createConstantString(
            @NotNull String name,
            @Nullable String value,
            @Nullable String accessModifier
    ) {
        final List<String> constantModifiers = new ArrayList<>();
        if (accessModifier != null) {
            constantModifiers.add(accessModifier);
        }
        constantModifiers.add(PsiModifier.STATIC);
        constantModifiers.add(PsiModifier.FINAL);
        final PsiExpression initializer = elementFactory.createExpressionFromText(
                StringUtils.quote(value), null
        );

        return createField(getStringType(), constantModifiers, name, initializer);
    }

    public PsiField createConstantString(@NotNull String name, @Nullable String value) {
        return createConstantString(name, value, null);
    }

    public PsiStatement createFieldInitialization(@NotNull String name,  @Nullable String value) {
        final String statementText;
        if (value != null) {
            final boolean namesEquals = Objects.equals(name, value);
            statementText = (namesEquals ? PsiKeyword.THIS + "." : "") + name + " = " + value + ";";
        } else {
            statementText = name + " = " + PsiKeyword.NULL + ";";
        }
        return elementFactory.createStatementFromText(statementText, null);
    }

    public void addAnnotation(@NotNull PsiParameter parameter, @NotNull String annotationText) {
        final PsiModifierList parameterModifiers = parameter.getModifierList();
        if (parameterModifiers != null) {
            final PsiAnnotation annotation = parameterModifiers.addAnnotation(annotationText);
            javaCodeStyleManager.shortenClassReferences(annotation);
        }
    }

    public void addAnnotation(@NotNull PsiMethod method, @NotNull String annotationText) {
        final PsiModifierList modifierList = method.getModifierList();
        final PsiAnnotation annotation = modifierList.addAnnotation(annotationText);
        javaCodeStyleManager.shortenClassReferences(annotation);
    }

    public PsiIfStatement createIfStatement(
            @NotNull PsiExpression condition,
            @NotNull List<PsiStatement> ifBranchStatements,
            @Nullable List<PsiStatement> elseBranchStatements
    ) {
        final PsiIfStatement ifElseStatement = (PsiIfStatement)elementFactory.createStatementFromText(
                "if(condition){}else{}", null
        );
        final PsiElement[] elements = ifElseStatement.getChildren();
        final PsiExpression ifElseCondition = ifElseStatement.getCondition();
        final PsiKeyword elseElement = ifElseStatement.getElseElement();

        if (ifElseCondition != null) {
            ifElseCondition.replace(condition);
        }

        final PsiElement ifBranchElement = elements[4].getFirstChild();
        for (PsiStatement ifBranchStatement : ifBranchStatements) {
            ifBranchElement.add(ifBranchStatement);
        }

        final PsiElement elseBranchElement = elements[6].getFirstChild();
        if (elseBranchStatements != null) {
            for (PsiStatement elseBranchStatement : elseBranchStatements) {
                elseBranchElement.add(elseBranchStatement);
            }
        } else {
            elseBranchElement.delete();
            elseElement.delete();
        }

        return ifElseStatement;
    }

    public PsiStatement createIfStatement(
            @Nullable PsiExpression ifCondition,
            @Nullable PsiStatement ifBranchStatement,
            @NotNull PsiExpression elseifCondition,
            @NotNull PsiStatement elseIfBranchStatement,
            @Nullable PsiStatement elseBranchStatement
    ) {
        final PsiStatement ifElseStatement = elementFactory.createStatementFromText(
                "if(c1){}else if(c2){}else{}", null
        );
        final PsiElement[] elements = ifElseStatement.getChildren();

        if (ifCondition != null) {
            elements[2].replace(ifCondition);
        }

        final PsiElement ifBranchElement = elements[4].getFirstChild();
        if (ifBranchStatement != null) {
            ifBranchElement.add(ifBranchStatement);
        }

        final PsiIfStatement elseIfStatement = createIfStatement(
                elseifCondition, elseIfBranchStatement, elseBranchStatement
        );
        elements[elements.length - 1].replace(elseIfStatement);

        return ifElseStatement;
    }

    public PsiIfStatement createIfStatement(
            @NotNull PsiExpression condition,
            @NotNull PsiStatement ifBranchStatement,
            @Nullable PsiStatement elseBranchStatement
    ) {
        final List<PsiStatement> ifBranchStatements = new ArrayList<>();
        final List<PsiStatement> elseBranchStatements;
        if (elseBranchStatement != null) {
            elseBranchStatements = new ArrayList<>();
            elseBranchStatements.add(elseBranchStatement);
        } else {
            elseBranchStatements = null;
        }
        ifBranchStatements.add(ifBranchStatement);
        return createIfStatement(condition, ifBranchStatements, elseBranchStatements);
    }

    public PsiExpression createTernaryExpression(
            @NotNull String condition,
            @NotNull String primaryStatement,
            @NotNull String secondaryStatement
    ) {
        final String ternaryStatement = condition + " ? " + primaryStatement + " : " + secondaryStatement;
        return elementFactory.createExpressionFromText(ternaryStatement, null);
    }

    public PsiExpression createInstanceOfExpression(@NotNull String targetObject, @NotNull String className) {
        final String instanceOf = targetObject + " " + PsiKeyword.INSTANCEOF + " " + className;

        return elementFactory.createExpressionFromText(instanceOf, null);
    }

    public PsiStatement createClassCastStatement(
            @NotNull String targetClassName, @NotNull String targetObject, @NotNull String currentObject
    ) {
        String classCastText = targetObject + " = " + "(" + targetClassName + ")" + currentObject + ";";

        return elementFactory.createStatementFromText(classCastText, null);
    }

    public PsiType getStringType() {
        return PsiType.getJavaLangString(psiManager, globalSearchScope);
    }
}
