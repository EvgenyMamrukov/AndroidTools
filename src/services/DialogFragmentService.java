package services;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.PsiUtils;
import utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DialogFragmentService extends AndroidService {

    private static final String REQUIRE_CONTEXT = "requireContext";
    private static final String REQUIRE_FRAGMENT_MANAGER = "requireFragmentManager";
    private static final String SUPPORT_FRAGMENT_MANAGER = "getSupportFragmentManager";
    private static final String GET_TARGET_REQUEST_CODE = "getTargetRequestCode";
    private static final String ALERT_DIALOG_BUILDER_NAME = "dialogBuilder";
    private static final String DIALOG_FRAGMENT_NAME = "dialogFragment";
    private static final String DIALOG_FRAGMENT_TAG = "dialogFragmentTag";
    private static final String FRAGMENT_MANAGER_NAME = "fragmentManager";

    private static final String BUILDER_CLASS_NAME = "Builder";
    private static final String CREATE_METHOD = "create";
    private static final String TARGET_ACTIVITY = "targetActivity";
    private static final String REQUIRE_ACTIVITY = "requireActivity";
    private static final String TARGET_FRAGMENT = "targetFragment";
    private static final String DIALOG_INTERFACE = "DialogFragmentInterface";
    private static final String DIALOG_INTERFACE_NAME = "dialogFragmentInterface";
    private static final String ON_POSITIVE_BUTTON_CLICKED = "onPositiveButtonClicked";
    private static final String ON_NEUTRAL_BUTTON_CLICKED = "onNeutralButtonClicked";
    private static final String ON_NEGATIVE_BUTTON_CLICKED = "onNegativeButtonClicked";
    private static final String GET_ARGUMENTS = "getArguments";
    private static final String SET_ARGUMENTS = "setArguments";
    private static final String SHOW_DIALOG = "showDialog";
    private static final String SHOW = "show";

    private static final String BUNDLE_INSTANCE = "new Bundle()";
    private static final String ARGUMENTS = "arguments";
    private static final String ARG_CANCELABLE = "ARG_CANCELABLE";
    private static final String ARG_TITLE_ID = "ARG_TITLE_ID";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_MESSAGE_ID = "ARG_MESSAGE_ID";
    private static final String ARG_MESSAGE = "ARG_MESSAGE";
    private static final String ARG_POSITIVE_BUTTON_TEXT_ID = "ARG_POSITIVE_BUTTON_TEXT_ID";
    private static final String ARG_POSITIVE_BUTTON_TEXT = "ARG_POSITIVE_BUTTON_TEXT";
    private static final String ARG_NEUTRAL_BUTTON_TEXT_ID = "ARG_NEUTRAL_BUTTON_TEXT_ID";
    private static final String ARG_NEUTRAL_BUTTON_TEXT = "ARG_NEUTRAL_BUTTON_TEXT";
    private static final String ARG_NEGATIVE_BUTTON_TEXT_ID = "ARG_NEGATIVE_BUTTON_TEXT_ID";
    private static final String ARG_NEGATIVE_BUTTON_TEXT = "ARG_NEGATIVE_BUTTON_TEXT";
    private static final String ARG_VIEW_RES_ID = "ARG_VIEW_RES_ID";

    private static final String TAG = "tag";
    private static final String TITLE = "title";
    private static final String TITLE_ID = "titleId";
    private static final String MESSAGE = "message";
    private static final String MESSAGE_ID = "messageId";
    private static final String CANCELABLE = "cancelable";
    private static final String POSITIVE_BUTTON = "positiveButton";
    private static final String POSITIVE_BUTTON_ID = "positiveButtonId";
    private static final String NEUTRAL_BUTTON = "neutralButton";
    private static final String NEUTRAL_BUTTON_ID = "neutralButtonId";
    private static final String NEGATIVE_BUTTON = "negativeButton";
    private static final String NEGATIVE_BUTTON_ID = "negativeButtonId";
    private static final String VIEW_RES_ID = "viewResId";

    private static final String BUTTON_LAMBDA_DIALOG = "dialog";
    private static final String BUTTON_LAMBDA_WHICH = "which";

    protected static final String[] DIALOG_FRAGMENT_METHOD_NAMES = {
            ON_CREATE_DIALOG,
            ON_START,
            ON_SAVE_INSTANCE_STATE,
            ON_STOP,
            ON_DISMISS
    };

    private static DialogFragmentService instance;

    private DialogFragmentService(@NotNull Project project) {
        super(project, ANDROIDX_DIALOG_FRAGMENT);
    }

    public static DialogFragmentService getInstance(@NotNull Project project) {
        if (instance == null) {
            instance = new DialogFragmentService(project);
        }

        return instance;
    }

    @Override
    protected String[] provideComponentMethodNames() {
        return DIALOG_FRAGMENT_METHOD_NAMES;
    }

    @Override
    protected List<PsiMethod> createComponentMethods(
            @NotNull PsiClass dialogFragmentClass, @NotNull List<PsiMethod> methods
    ) {
        final List<PsiMethod> generatedMethods = new ArrayList<>();
        for (PsiMethod method : methods) {
            switch (method.getName()) {
                case ON_CREATE_DIALOG:
                    dialogFragmentClass.add(psiUtils.createConstructor(
                            dialogFragmentClass, PsiModifier.PUBLIC, null)
                    );
                    dialogFragmentClass.add(createDialogFragmentInterface());
                    dialogFragmentClass.add(createDialogFragmentBuilder(dialogFragmentClass));
                    generatedMethods.add(overrideOnCreateDialogMethod(method));
                    break;
                case ON_START:
                case ON_SAVE_INSTANCE_STATE:
                case ON_STOP:
                case ON_DISMISS:
                    generatedMethods.add(psiUtils.overrideMethod(method));
                    break;
            }
        }

        return generatedMethods;
    }

    private PsiMethod overrideOnCreateDialogMethod(@NotNull PsiMethod componentMethod) {
        final PsiMethod onCreateDialogMethod = psiUtils.overrideMethod(componentMethod, false);
        final PsiCodeBlock codeBlock = onCreateDialogMethod.getBody();

        if (codeBlock != null) {
            applyDialogBuilderArguments(codeBlock);
            addDialogBuilderReturnStatement(codeBlock);
        }

        return onCreateDialogMethod;
    }

    private void applyDialogBuilderArguments(@NotNull PsiCodeBlock onCreateDialogMethodBody) {
        final PsiClass alertDialogBuilderClass = javaFacade.findClass(ANDROIDX_ALERT_DIALOG_BUILDER, globalSearchScope);
        final PsiClass bundleClass = javaFacade.findClass(BUNDLE, globalSearchScope);

        if (alertDialogBuilderClass != null && bundleClass != null) {
            final PsiType alertDialogBuilderType = elementFactory.createType(alertDialogBuilderClass);
            final PsiType bundleType = elementFactory.createType(bundleClass);

            final String requireContextCall = psiUtils.createMethodCall(
                    REQUIRE_CONTEXT, null, null, false
            );
            final List<String> createDialogParams = new ArrayList<>();
            createDialogParams.add(requireContextCall);
            final PsiStatement createAlertDialogBuilder = psiUtils.createVariable(
                    alertDialogBuilderType, ALERT_DIALOG_BUILDER_NAME, createDialogParams
            );
            onCreateDialogMethodBody.add(createAlertDialogBuilder);

            final String getArgumentsMethodCall = psiUtils.createMethodCall(
                    GET_ARGUMENTS, null, null, false
            );
            final PsiExpression getArgumentsExpression = elementFactory.createExpressionFromText(
                    getArgumentsMethodCall, null
            );
            final PsiStatement getArgumentsStatement = psiUtils.createVariable(
                    getArgumentsExpression, bundleType, ARGUMENTS
            );
            onCreateDialogMethodBody.add(getArgumentsStatement);

            final String argumentsCheckText = ARGUMENTS + "!=" + PsiKeyword.NULL;
            final PsiExpression argumentsCheck = elementFactory.createExpressionFromText(
                    argumentsCheckText, null
            );
            final List<PsiStatement> argumentsIfBranch = new ArrayList<>();

            addDialogTextContent(argumentsIfBranch, ARG_TITLE_ID, TITLE_ID, ARG_TITLE, TITLE);
            addDialogTextContent(argumentsIfBranch, ARG_MESSAGE_ID, MESSAGE_ID, ARG_MESSAGE, MESSAGE);
            addDialogButtonListeners(argumentsIfBranch);
            final PsiIfStatement setArgumentsBlock = psiUtils.createIfStatement(
                    argumentsCheck, argumentsIfBranch, null
            );

            onCreateDialogMethodBody.add(setArgumentsBlock);
        }
    }

    private void addDialogTextContent(
            @NotNull List<PsiStatement> argumentsBlockStatements,
            @NotNull String textIdKey,
            @NotNull String textId,
            @NotNull String textKey,
            @NotNull String text
    ) {
        final PsiStatement getStringIdStatement = createGetFromBundleVariable(
                ARGUMENTS, PsiType.INT, textId, textIdKey, "0"
        );
        argumentsBlockStatements.add(getStringIdStatement);

        final String setConditionText = textId + " != " + "0";
        final PsiExpression setCondition = elementFactory.createExpressionFromText(setConditionText, null);
        final List<String> setTextIdParams = new ArrayList<>();
        setTextIdParams.add(textId);

        final List<PsiStatement> setTextIfBranch = new ArrayList<>();
        final List<PsiStatement> setTextElseBranch = new ArrayList<>();
        final String setContentName = PsiUtils.SET + StringUtils.capitalize(text);
        final String setTextId = psiUtils.createMethodCall(
                setContentName, setTextIdParams, ALERT_DIALOG_BUILDER_NAME, false
        );
        final PsiStatement setTextIdStatement = elementFactory.createStatementFromText(
                setTextId + ";", null
        );
        setTextIfBranch.add(setTextIdStatement);

        final PsiStatement getTextStatement = createGetFromBundleVariable(
                ARGUMENTS, psiUtils.getStringType(), text, textKey, null
        );
        setTextElseBranch.add(getTextStatement);

        final PsiExpression textIsEmptyCheck = textIsEmptyCheck(text, true);
        final List<String> setTextParams = new ArrayList<>();
        setTextParams.add(text);
        final String setText = psiUtils.createMethodCall(
                setContentName, setTextParams, ALERT_DIALOG_BUILDER_NAME, false
        );
        final PsiStatement setTextStatement = elementFactory.createStatementFromText(
                setText + ";", null
        );
        final PsiIfStatement ifTextNotEmpty = psiUtils.createIfStatement(
                textIsEmptyCheck, setTextStatement, null
        );
        setTextElseBranch.add(ifTextNotEmpty);

        final PsiStatement textContentStatement = psiUtils.createIfStatement(
                setCondition, setTextIfBranch, setTextElseBranch
        );
        argumentsBlockStatements.add(textContentStatement);
    }

    private void addDialogButtonListeners(@NotNull List<PsiStatement> argumentsBlockStatements) {
        argumentsBlockStatements.addAll(createDialogInterfaceReference());
        argumentsBlockStatements.addAll(
                createDialogButton(
                        ON_POSITIVE_BUTTON_CLICKED,
                        POSITIVE_BUTTON,
                        POSITIVE_BUTTON_ID,
                        ARG_POSITIVE_BUTTON_TEXT_ID,
                        POSITIVE_BUTTON,
                        ARG_POSITIVE_BUTTON_TEXT
                ));
        argumentsBlockStatements.addAll(
                createDialogButton(
                        ON_NEUTRAL_BUTTON_CLICKED,
                        NEUTRAL_BUTTON,
                        NEUTRAL_BUTTON_ID,
                        ARG_NEUTRAL_BUTTON_TEXT_ID,
                        NEUTRAL_BUTTON,
                        ARG_NEUTRAL_BUTTON_TEXT
                ));
        argumentsBlockStatements.addAll(
                createDialogButton(
                        ON_NEGATIVE_BUTTON_CLICKED,
                        NEGATIVE_BUTTON,
                        NEGATIVE_BUTTON_ID,
                        ARG_NEGATIVE_BUTTON_TEXT_ID,
                        NEGATIVE_BUTTON,
                        ARG_NEGATIVE_BUTTON_TEXT
                ));
    }

    private List<PsiStatement> createDialogButton(
            @NotNull String dialogInterfaceMethodName,
            @NotNull String dialogButtonType,
            @NotNull String buttonTextId,
            @NotNull String textIdKey,
            @NotNull String buttonText,
            @NotNull String textKey
    ) {
        final List<PsiStatement> buttonStatements = new ArrayList<>();
        final PsiStatement getStringIdStatement = createGetFromBundleVariable(
                ARGUMENTS, PsiType.INT, buttonTextId, textIdKey, "0"
        );
        buttonStatements.add(getStringIdStatement);

        final List<String> lambdaParams = new ArrayList<>();
        final List<PsiStatement> lambdaStatements = new ArrayList<>();
        lambdaParams.add(BUTTON_LAMBDA_DIALOG);
        lambdaParams.add(BUTTON_LAMBDA_WHICH);

        final String dialogInterfaceConditionText = DIALOG_INTERFACE_NAME + " != " + PsiKeyword.NULL;
        final PsiExpression dialogInterfaceCondition = elementFactory.createExpressionFromText(
                dialogInterfaceConditionText, null
        );
        final String dialogInterfaceMethodCallText = psiUtils.createMethodCall(
                dialogInterfaceMethodName, null, DIALOG_INTERFACE_NAME, false
        ) + ";";
        final PsiStatement dialogInterfaceMethodCall = elementFactory.createStatementFromText(
                dialogInterfaceMethodCallText, null
        );
        final PsiIfStatement dialogInterfaceStatement = psiUtils.createIfStatement(
                dialogInterfaceCondition, dialogInterfaceMethodCall, null
        );
        lambdaStatements.add(dialogInterfaceStatement);
        final String lambda = psiUtils.createLambda(lambdaParams, lambdaStatements);

        final String setButtonMethodName = PsiUtils.SET + StringUtils.capitalize(dialogButtonType);

        final List<String> setButtonTextIdParams = new ArrayList<>();
        setButtonTextIdParams.add(buttonTextId);
        setButtonTextIdParams.add(lambda);

        final List<String> setButtonTextParams = new ArrayList<>();
        setButtonTextParams.add(buttonText);
        setButtonTextParams.add(lambda);

        final PsiExpression checkButtonId = elementFactory.createExpressionFromText(
                buttonTextId + " != 0", null
        );

        final List<PsiStatement> setButtonIfBranch = new ArrayList<>();
        final List<PsiStatement> setButtonElseBranch = new ArrayList<>();

        final String setButtonTextIdCall = psiUtils.createMethodCall(
                setButtonMethodName, setButtonTextIdParams, ALERT_DIALOG_BUILDER_NAME, false
        ) + ";";
        setButtonIfBranch.add(elementFactory.createStatementFromText(setButtonTextIdCall, null));

        final PsiStatement getStringStatement = createGetFromBundleVariable(
                ARGUMENTS, psiUtils.getStringType(), buttonText, textKey, null
        );
        setButtonElseBranch.add(getStringStatement);
        final String setButtonTextCall = psiUtils.createMethodCall(
                setButtonMethodName, setButtonTextParams, ALERT_DIALOG_BUILDER_NAME, false
        ) + ";";
        setButtonElseBranch.add(elementFactory.createStatementFromText(setButtonTextCall, null));

        final PsiIfStatement setButtonBlock = psiUtils.createIfStatement(
                checkButtonId, setButtonIfBranch, setButtonElseBranch
        );
        buttonStatements.add(setButtonBlock);

        return buttonStatements;
    }

    private List<PsiStatement> createDialogInterfaceReference() {
        final List<PsiStatement> initializationStatements = new ArrayList<>();

        final PsiClass dialogInterface = elementFactory.createClass(DIALOG_INTERFACE);
        final PsiType dialogInterfaceType = elementFactory.createType(dialogInterface);
        final PsiStatement dialogInterfaceVariable = psiUtils.createVariable(
                null, dialogInterfaceType, DIALOG_INTERFACE_NAME
        );
        initializationStatements.add(dialogInterfaceVariable);

        final String requireActivityCall = psiUtils.createMethodCall(
                REQUIRE_ACTIVITY, null, null, false
        );
        final PsiExpression dialogInterfaceIfExpression = psiUtils.createInstanceOfExpression(
                requireActivityCall, DIALOG_INTERFACE
        );
        final PsiStatement dialogInterfaceIfBranch = psiUtils.createClassCastStatement(
                DIALOG_INTERFACE, DIALOG_INTERFACE_NAME, requireActivityCall
        );

        final String targetFragmentCall = psiUtils.createMethodCall(
                PsiUtils.GET + StringUtils.capitalize(TARGET_FRAGMENT),
                null,
                null,
                false
        );
        final PsiExpression dialogInterfaceElseIfExpression = psiUtils.createInstanceOfExpression(
                targetFragmentCall, DIALOG_INTERFACE
        );
        final PsiStatement dialogInterfaceElseIfBranch = psiUtils.createClassCastStatement(
                DIALOG_INTERFACE, DIALOG_INTERFACE_NAME, targetFragmentCall
        );
        final PsiStatement dialogInterfaceElseBranch = elementFactory.createStatementFromText(
                DIALOG_INTERFACE_NAME + " = null;", null
        );

        final PsiStatement dialogInterfaceInitialization = psiUtils.createIfStatement(
                dialogInterfaceIfExpression,
                dialogInterfaceIfBranch,
                dialogInterfaceElseIfExpression,
                dialogInterfaceElseIfBranch,
                dialogInterfaceElseBranch
        );
        initializationStatements.add(dialogInterfaceInitialization);

        return initializationStatements;
    }

    private void addDialogBuilderReturnStatement(@NotNull PsiCodeBlock onCreateDialogMethodBody) {
        final String createMethodCall = psiUtils.createMethodCall(
                CREATE_METHOD, null, null, false
        );
        final String returnStatementText
                = PsiKeyword.RETURN + " " + ALERT_DIALOG_BUILDER_NAME + "." + createMethodCall + ";";
        onCreateDialogMethodBody.add(elementFactory.createStatementFromText(returnStatementText, null));
    }

    private PsiClass createDialogFragmentInterface() {
        final PsiClass dialogListener = elementFactory.createInterface(DIALOG_INTERFACE);
        final PsiMethod onPositiveButtonClicked = psiUtils.createInterfaceMethod(
                ON_POSITIVE_BUTTON_CLICKED, PsiType.VOID, null
        );
        final PsiMethod onNeutralButtonClicked = psiUtils.createInterfaceMethod(
                ON_NEUTRAL_BUTTON_CLICKED, PsiType.VOID, null
        );
        final PsiMethod onNegativeButtonClicked = psiUtils.createInterfaceMethod(
                ON_NEGATIVE_BUTTON_CLICKED, PsiType.VOID, null
        );
        dialogListener.add(onPositiveButtonClicked);
        dialogListener.add(onNeutralButtonClicked);
        dialogListener.add(onNegativeButtonClicked);

        return dialogListener;
    }

    private PsiClass createDialogFragmentBuilder(@NotNull PsiClass dialogFragmentClass) {
        final PsiClass builderClass = elementFactory.createClass(BUILDER_CLASS_NAME);
        final PsiModifierList builderClassModifiers = builderClass.getModifierList();
        if (builderClassModifiers != null) {
            builderClassModifiers.setModifierProperty(PsiModifier.PUBLIC, true);
            builderClassModifiers.setModifierProperty(PsiModifier.STATIC, true);
        }

        addDialogBuilderConstructors(builderClass);
        addDialogBuilderSetters(dialogFragmentClass, builderClass);
        builderClass.add(createDialogMethod(dialogFragmentClass));
        builderClass.add(createShowDialogMethod(dialogFragmentClass));

        return builderClass;
    }

    private PsiMethod createBuilderConstructor(@NotNull PsiClass builderClass, @NotNull PsiClass componentClass) {
        final PsiClass activityClass = javaFacade.findClass(ANDROIDX_ACTIVITY, globalSearchScope);
        final PsiClass fragmentClass = javaFacade.findClass(ANDROIDX_FRAGMENT, globalSearchScope);
        final PsiType componentType = elementFactory.createType(componentClass);
        final PsiParameter componentParameter;
        final String primaryComponent;
        final String secondaryComponent;

        if (componentClass.isEquivalentTo(activityClass)) {
            componentParameter = elementFactory.createParameter(TARGET_ACTIVITY, componentType);
            primaryComponent = TARGET_ACTIVITY;
            secondaryComponent = TARGET_FRAGMENT;
        } else if (componentClass.isEquivalentTo(fragmentClass)) {
            componentParameter = elementFactory.createParameter(TARGET_FRAGMENT, componentType);
            primaryComponent = TARGET_FRAGMENT;
            secondaryComponent = TARGET_ACTIVITY;
        } else {
            return elementFactory.createConstructor();
        }

        psiUtils.addAnnotation(componentParameter, ANDROIDX_NON_NULL);
        final List<PsiParameter> parameters = new ArrayList<>();
        parameters.add(componentParameter);
        final PsiMethod constructor = psiUtils.createConstructor(builderClass, PsiModifier.PUBLIC, parameters);

        final List<PsiStatement> fieldInitializationList = new ArrayList<>();
        fieldInitializationList.add(psiUtils.createFieldInitialization(primaryComponent, primaryComponent));
        fieldInitializationList.add(psiUtils.createFieldInitialization(secondaryComponent, null));

        psiUtils.addMethodBody(constructor, fieldInitializationList);

        return constructor;
    }

    private PsiMethod createBuilderSetter(
            @Nullable PsiType returnType,
            @NotNull PsiType argumentType,
            @NotNull String argumentName,
            @NotNull String argumentKey,
            @Nullable String annotation
    ) {
        final PsiParameter setterParameter = elementFactory.createParameter(argumentName, argumentType);
        if (annotation != null) {
            psiUtils.addAnnotation(setterParameter, annotation);
        }
        final String setterName = PsiUtils.SET + StringUtils.capitalize(argumentName);
        final PsiMethod setter = psiUtils.createMethod(
                setterName, PsiModifier.PUBLIC, returnType, new PsiParameter[] {setterParameter}
                );
        final List<PsiStatement> setterStatements = new ArrayList<>();
        setterStatements.add(createPutToBundleStatement(ARGUMENTS, argumentType, argumentName, argumentKey));
        setterStatements.add(
                elementFactory.createStatementFromText(
                        PsiKeyword.RETURN + " " + PsiKeyword.THIS + ";", null
                )
        );
        psiUtils.addMethodBody(setter, setterStatements);

        return setter;
    }

    private PsiMethod createDialogMethod(@NotNull PsiClass dialogFragmentClass) {
        final PsiType dialogFragmentType = elementFactory.createType(dialogFragmentClass);
        final PsiMethod createDialogMethod = psiUtils.createMethod(
                CREATE_METHOD, PsiModifier.PUBLIC, dialogFragmentType, null
        );
        final PsiCodeBlock methodBody = createDialogMethod.getBody();
        if (methodBody != null) {
            final PsiStatement createDialogFragment = psiUtils.createVariable(
                    dialogFragmentType, DIALOG_FRAGMENT_NAME, null
            );
            methodBody.add(createDialogFragment);

            final List<String> setArgumentsParams = new ArrayList<>();
            setArgumentsParams.add(ARGUMENTS);
            final String setArguments = psiUtils.createMethodCall(
                    SET_ARGUMENTS, setArgumentsParams, DIALOG_FRAGMENT_NAME, false
            ) + ";";
            final PsiStatement setArgumentsStatement = elementFactory.createStatementFromText(
                    setArguments, null
            );
            methodBody.add(setArgumentsStatement);

            final String getTargetRequestCode = psiUtils.createMethodCall(
                    GET_TARGET_REQUEST_CODE, null, DIALOG_FRAGMENT_NAME, false
            );
            final String setTargetFragmentName = PsiUtils.SET + StringUtils.capitalize(TARGET_FRAGMENT);
            final List<String> setTargetFragmentParams = new ArrayList<>();
            setTargetFragmentParams.add(TARGET_FRAGMENT);
            setTargetFragmentParams.add(getTargetRequestCode);
            final PsiExpression targetFragmentCondition = elementFactory.createExpressionFromText(
                    TARGET_FRAGMENT + " != " + PsiKeyword.NULL, null
            );
            final String setTargetFragmentMethodText = psiUtils.createMethodCall(
                    setTargetFragmentName, setTargetFragmentParams, DIALOG_FRAGMENT_NAME, false
            );
            final PsiStatement setTargetFragmentMethod = elementFactory.createStatementFromText(
                    setTargetFragmentMethodText + ";", null
            );
            final PsiIfStatement setTargetFragmentStatement = psiUtils.createIfStatement(
                    targetFragmentCondition, setTargetFragmentMethod, null
            );
            methodBody.add(setTargetFragmentStatement);

            final String returnStatementText = PsiKeyword.RETURN + " " + DIALOG_FRAGMENT_NAME + ";";
            final PsiStatement returnStatement = elementFactory.createStatementFromText(
                    returnStatementText, null
            );
            methodBody.add(returnStatement);
        }

        return createDialogMethod;
    }

    private PsiMethod createShowDialogMethod(@NotNull PsiClass dialogFragmentClass) {
        final PsiType dialogFragmentType = elementFactory.createType(dialogFragmentClass);
        final PsiParameter tagParameter = elementFactory.createParameter(TAG, psiUtils.getStringType());
        psiUtils.addAnnotation(tagParameter, ANDROIDX_NULLABLE);
        final PsiMethod method = psiUtils.createMethod(
                SHOW_DIALOG, PsiModifier.PUBLIC, PsiType.VOID, new PsiParameter[]{tagParameter}
                );

        final String dialogFragmentClassName = dialogFragmentClass.getName();
        if (dialogFragmentClassName != null) {
            final String dialogTagName = StringUtils.convertCamelToSnake(
                    dialogFragmentClassName, null, TAG, true
            );
            dialogFragmentClass.add(
                    psiUtils.createConstantString(dialogTagName, dialogTagName.toLowerCase(), PsiModifier.PUBLIC)
            );

            final PsiCodeBlock methodBody = method.getBody();
            if (methodBody != null) {
                final PsiExpression nullCheckCondition = elementFactory.createExpressionFromText(
                        TARGET_FRAGMENT
                            + " == "
                            + PsiKeyword.NULL
                            + " && "
                            + TARGET_ACTIVITY
                            + " == "
                            + PsiKeyword.NULL,
                null
                );
                final PsiStatement checkConditionIfBranch = elementFactory.createStatementFromText(
                        PsiKeyword.RETURN + ";", null
                );
                final PsiIfStatement nullCheckStatement = psiUtils.createIfStatement(
                        nullCheckCondition, checkConditionIfBranch, null
                );
                methodBody.add(nullCheckStatement);

                final PsiClass fragmentManagerClass = javaFacade.findClass(
                        ANDROIDX_FRAGMENT_MANAGER, globalSearchScope
                );
                final String targetFragmentConditionText = TARGET_FRAGMENT + " != " + PsiKeyword.NULL;
                if (fragmentManagerClass != null) {
                    final PsiType fragmentManagerType = elementFactory.createType(fragmentManagerClass);
                    final String requireFragmentManager = psiUtils.createMethodCall(
                            REQUIRE_FRAGMENT_MANAGER, null, TARGET_FRAGMENT, false
                    );
                    final String supportFragmentManager = psiUtils.createMethodCall(
                            SUPPORT_FRAGMENT_MANAGER, null, TARGET_ACTIVITY, false
                    );

                    final PsiExpression fragmentManagerInitializer = psiUtils.createTernaryExpression(
                            targetFragmentConditionText, requireFragmentManager, supportFragmentManager
                    );
                    final PsiStatement createFragmentManager = psiUtils.createVariable(
                            fragmentManagerInitializer, fragmentManagerType, FRAGMENT_MANAGER_NAME
                    );
                    methodBody.add(createFragmentManager);
                }

                final String tagCondition = TAG + " != " + PsiKeyword.NULL;
                final PsiExpression tagInitializer = psiUtils.createTernaryExpression(
                        tagCondition, TAG, dialogTagName
                );
                final PsiStatement dialogFragmentTag = psiUtils.createVariable(
                        tagInitializer, psiUtils.getStringType(), DIALOG_FRAGMENT_TAG
                );
                methodBody.add(dialogFragmentTag);

                final PsiStatement createDialogFragment = psiUtils.createVariable(
                        dialogFragmentType, DIALOG_FRAGMENT_NAME, null
                );
                methodBody.add(createDialogFragment);

                final List<String> setArgumentsParams = new ArrayList<>();
                setArgumentsParams.add(ARGUMENTS);
                final String setArguments = psiUtils.createMethodCall(
                        SET_ARGUMENTS, setArgumentsParams, DIALOG_FRAGMENT_NAME, false
                ) + ";";
                final PsiStatement setArgumentsStatement = elementFactory.createStatementFromText(
                        setArguments, null
                );
                methodBody.add(setArgumentsStatement);

                final String getTargetRequestCode = psiUtils.createMethodCall(
                        GET_TARGET_REQUEST_CODE, null, DIALOG_FRAGMENT_NAME, false
                );
                final String setTargetFragmentName = PsiUtils.SET + StringUtils.capitalize(TARGET_FRAGMENT);
                final List<String> setTargetFragmentParams = new ArrayList<>();
                setTargetFragmentParams.add(TARGET_FRAGMENT);
                setTargetFragmentParams.add(getTargetRequestCode);
                final String setTargetFragmentMethodText = psiUtils.createMethodCall(
                        setTargetFragmentName, setTargetFragmentParams, DIALOG_FRAGMENT_NAME, false
                );
                final PsiStatement setTargetFragmentMethodCall = elementFactory.createStatementFromText(
                        setTargetFragmentMethodText + ";", null
                );
                final PsiExpression targetFragmentCondition = elementFactory.createExpressionFromText(
                        targetFragmentConditionText, null
                );
                final PsiIfStatement setTargetFragmentStatement = psiUtils.createIfStatement(
                        targetFragmentCondition, setTargetFragmentMethodCall, null
                );
                methodBody.add(setTargetFragmentStatement);

                final List<String> showDialogParams = new ArrayList<>();
                showDialogParams.add(FRAGMENT_MANAGER_NAME);
                showDialogParams.add(DIALOG_FRAGMENT_TAG);
                final String showDialogFragment = psiUtils.createMethodCall(
                        SHOW, showDialogParams, DIALOG_FRAGMENT_NAME, false
                ) + ";";
                final PsiStatement showDialogFragmentStatement = elementFactory.createStatementFromText(
                        showDialogFragment, null
                );
                methodBody.add(showDialogFragmentStatement);
            }
        }

        return method;
    }

    private void addDialogBuilderConstructors(@NotNull PsiClass builderClass) {
        final PsiClass activityClass = javaFacade.findClass(ANDROIDX_ACTIVITY, globalSearchScope);
        final PsiClass fragmentClass = javaFacade.findClass(ANDROIDX_FRAGMENT, globalSearchScope);
        final PsiClass bundleClass = javaFacade.findClass(BUNDLE, globalSearchScope);

        if (activityClass == null || fragmentClass == null || bundleClass == null) {
            return;
        }

        final List<String> builderFieldModifiers = new ArrayList<>();
        builderFieldModifiers.add(PsiModifier.FINAL);
        final List<FieldData> fieldDataList = new ArrayList<>();

        fieldDataList.add(
                new FieldData(
                        elementFactory.createType(activityClass),
                        builderFieldModifiers,
                        TARGET_ACTIVITY,
                        null
                )
        );
        fieldDataList.add(
                new FieldData(
                        elementFactory.createType(fragmentClass),
                        builderFieldModifiers,
                        TARGET_FRAGMENT,
                        null
                )
        );
        final PsiExpression bundleInitializer = elementFactory.createExpressionFromText(BUNDLE_INSTANCE, builderClass);
        fieldDataList.add(
                new FieldData(
                        elementFactory.createType(bundleClass),
                        builderFieldModifiers,
                        ARGUMENTS,
                        bundleInitializer
                )
        );

        for (FieldData fieldData : fieldDataList) {
            builderClass.add(
                    psiUtils.createField(
                            fieldData.fieldType,
                            fieldData.modifiers,
                            fieldData.filedName,
                            fieldData.initializer
                    )
            );
        }

        final PsiMethod primaryConstructor = createBuilderConstructor(builderClass, activityClass);
        final PsiMethod secondaryConstructor = createBuilderConstructor(builderClass, fragmentClass);

        builderClass.add(primaryConstructor);
        builderClass.add(secondaryConstructor);
    }

    private void addDialogBuilderSetters(@NotNull PsiClass dialogFragmentClass, @NotNull PsiClass builderClass) {
        final PsiType builderType = elementFactory.createType(builderClass);
        final List<SetterData> setterDataList = new ArrayList<>();

        setterDataList.add(new SetterData(builderType, PsiType.INT, TITLE, ARG_TITLE_ID, ANDROIDX_STRING_RES));
        setterDataList.add(new SetterData(builderType, psiUtils.getStringType(), TITLE, ARG_TITLE, ANDROIDX_NULLABLE));
        setterDataList.add(new SetterData(builderType, PsiType.INT, MESSAGE, ARG_MESSAGE_ID, ANDROIDX_STRING_RES));
        setterDataList.add(
                new SetterData(builderType, psiUtils.getStringType(), MESSAGE, ARG_MESSAGE, ANDROIDX_NULLABLE)
        );
        setterDataList.add(
                new SetterData(builderType,
                        PsiType.INT,
                        POSITIVE_BUTTON,
                        ARG_POSITIVE_BUTTON_TEXT_ID,
                        ANDROIDX_STRING_RES
                )
        );
        setterDataList.add(
                new SetterData(
                        builderType,
                        psiUtils.getStringType(),
                        POSITIVE_BUTTON,
                        ARG_POSITIVE_BUTTON_TEXT,
                        ANDROIDX_NULLABLE
                )
        );
        setterDataList.add(
                new SetterData(
                        builderType,
                        PsiType.INT,
                        NEUTRAL_BUTTON,
                        ARG_NEUTRAL_BUTTON_TEXT_ID,
                        ANDROIDX_STRING_RES
                )
        );
        setterDataList.add(
                new SetterData(
                        builderType,
                        psiUtils.getStringType(),
                        NEUTRAL_BUTTON,
                        ARG_NEUTRAL_BUTTON_TEXT,
                        ANDROIDX_NULLABLE
                )
        );
        setterDataList.add(
                new SetterData(
                        builderType,
                        PsiType.INT,
                        NEGATIVE_BUTTON,
                        ARG_NEGATIVE_BUTTON_TEXT_ID,
                        ANDROIDX_STRING_RES
                )
        );
        setterDataList.add(
                new SetterData(
                        builderType,
                        psiUtils.getStringType(),
                        NEGATIVE_BUTTON,
                        ARG_NEGATIVE_BUTTON_TEXT,
                        ANDROIDX_NULLABLE
                )
        );
        setterDataList.add(
                new SetterData(
                        builderType,
                        PsiType.BOOLEAN,
                        CANCELABLE,
                        ARG_CANCELABLE,
                        null
                )
        );
        setterDataList.add(
                new SetterData(
                        builderType,
                        PsiType.INT,
                        VIEW_RES_ID,
                        ARG_VIEW_RES_ID,
                        ANDROIDX_LAYOUT_RES
                )
        );

        for (SetterData setterData : setterDataList) {
            final String key = setterData.argumentKey;
            dialogFragmentClass.add(psiUtils.createConstantString(key, key.toLowerCase()));
            builderClass.add(
                    createBuilderSetter(
                            setterData.returnType,
                            setterData.argumentType,
                            setterData.argumentName,
                            key,
                            setterData.annotation
                    )
            );
        }
    }

    private static class FieldData {
        private final PsiType fieldType;
        private final List<String> modifiers;
        private final String filedName;
        private final PsiExpression initializer;

        private FieldData(
                @NotNull PsiType fieldType,
                @Nullable List<String> modifiers,
                @NotNull String fieldName,
                @Nullable PsiExpression initializer
        ) {
            this.fieldType = fieldType;
            this.modifiers = modifiers;
            this.filedName = fieldName;
            this.initializer = initializer;
        }
    }

    private static class SetterData {
        private final PsiType returnType;
        private final PsiType argumentType;
        private final String argumentName;
        private final String argumentKey;
        private final String annotation;

        private SetterData(
                @Nullable PsiType returnType,
                @NotNull PsiType argumentType,
                @NotNull String argumentName,
                @NotNull String argumentKey,
                @Nullable String annotation
        ) {
            this.returnType = returnType;
            this.argumentType = argumentType;
            this.argumentName = argumentName;
            this.argumentKey = argumentKey;
            this.annotation = annotation;
        }
    }
}
