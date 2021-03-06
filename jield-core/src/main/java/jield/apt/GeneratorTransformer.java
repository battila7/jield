package jield.apt;

import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static javax.lang.model.element.Modifier.STATIC;
import static jield.apt.Continuation.NO_LABEL;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import java.util.*;

/*
 * Class that does the heavy-lifting of the transformation process. Takes a method and its enclosing class
  * and creates a nested generator class and rewrites the original method.
 */
final class GeneratorTransformer {
    private static final String CLASS_NAME_PREFIX = "$GeneratorImpl_";

    private static final String CONTINUATION_PARAM = "$_contParam";

    private static final String GENERATOR_VARIABLE = "$generator";

    private static final long NO_MODIFIERS = 0L;

    private final String methodNamePrefix;

    private final JCClassDecl enclosingClass;

    private final JCMethodDecl originalMethod;

    private final ProcessingContext ctx;

    private final String className;

    private final Map<Integer, java.util.List<JCStatement>> states;

    private final Map<String, JCVariableDecl> fields;

    private final java.util.List<JCTree> classDefs;

    private final JCExpression generatedType;

    private final boolean isStaticContext;

    private String selfName;

    private int maxState;

    private int endState;

    GeneratorTransformer(JCClassDecl enclosingClass, JCMethodDecl originalMethod, ProcessingContext ctx, int index) {
        this.enclosingClass = enclosingClass;

        this.originalMethod = originalMethod;

        this.ctx = ctx;

        this.methodNamePrefix = generateMethodNamePrefix();

        this.className = generateClassName(index);

        this.states = new HashMap<>();

        this.fields = new HashMap<>();

        this.classDefs = new ArrayList<>();

        this.isStaticContext =
                originalMethod.getModifiers().getFlags().contains(STATIC) || INTERFACE.equals(enclosingClass.getKind());

        this.maxState = -1;

        /*
         * Here we can safely assume that the return type has at least one (actually it has exactly
         * one) type parameter because of previous checks.
         */
        this.generatedType = ((JCTypeApply) originalMethod.getReturnType()).arguments.get(0);
    }

    private String generateMethodNamePrefix() {
        Random rand = new Random();

        return "$" + originalMethod.getName() + "_" + Integer.toString(rand.nextInt()) + "_";
    }

    private String generateClassName(int index) {
        Random rand = new Random();

        String name = CLASS_NAME_PREFIX + Integer.toString(index);

        name += originalMethod.getName().toString();

        name += Integer.toString(rand.nextInt());

        return name;
    }

    /**
     * Performs the transformation on the method passed to the constructor. Destructive in the sense that the original implementation if the
     * method will not be preserved but rewritten to appropriate calls to the new nested generator class.
     * @return a new class that contains the generator logic
     */
    JCClassDecl transform() {
        final int startState = newState();

        this.endState = newState();

        if (!isStaticContext) {
            generateSelfField();
        }

        generateParameterFields();

        transformBlock(originalMethod.getBody(), startState,
                Continuation.empty().nextCont(endState).rename("this", this.selfName));

        finishEndState();

        return createClassDeclaration();
    }

    private void generateSelfField() {
        Random rand = new Random();

        this.selfName = "$" + enclosingClass.getSimpleName().toString() + Integer.toString(rand.nextInt());

        JCVariableDecl decl =
            ctx.treeMaker.VarDef(ctx.treeMaker.Modifiers(Flags.PRIVATE),
                                 ctx.name(this.selfName), ctx.treeMaker.Ident(enclosingClass.name),
                                 null);

        fields.put(this.selfName, decl);
    }

    /**
     * Fills the end state with code. Generates the following code:
     * <pre>
     * {@code
     *     return cont(() -> k.apply(GeneratorState.empty()));
     * }
     * </pre>
     */
    private void finishEndState() {
        final JCIdent jieldPackage =
                ctx.treeMaker.Ident(ctx.names.fromString(Identifiers.JIELD));

        final JCFieldAccess runtimeAccess =
                ctx.treeMaker.Select(jieldPackage, ctx.names.fromString(Identifiers.RUNTIME));

        final JCFieldAccess genStateAccess =
                ctx.treeMaker.Select(runtimeAccess, ctx.names.fromString(Identifiers.GENERATOR_STATE));

        final JCExpression selectEmpty =
            ctx.treeMaker.Select(genStateAccess, ctx.name(Identifiers.EMPTY_METHOD));

        final JCMethodInvocation invokeEmpty =
            ctx.treeMaker.Apply(List.nil(), selectEmpty.setType(Type.noType), List.nil());

        final JCExpression selectApply =
            ctx.treeMaker.Select(ctx.treeMaker.Ident(ctx.name(CONTINUATION_PARAM)), ctx.name(Identifiers.APPLY_METHOD));

        final JCMethodInvocation invokeApply =
            ctx.treeMaker.App(selectApply.setType(Type.noType), List.of(invokeEmpty));

        final JCLambda lambda = ctx.treeMaker.Lambda(List.nil(), invokeApply);

        final JCFieldAccess bounceAccess =
                ctx.treeMaker.Select(runtimeAccess, ctx.names.fromString(Identifiers.BOUNCE));

        final JCFieldAccess contAccess =
                ctx.treeMaker.Select(bounceAccess, ctx.names.fromString(Identifiers.CONT_METHOD));

        final JCMethodInvocation invokeCont =
            ctx.treeMaker.App(contAccess.setType(Type.noType), List.of(lambda));

        final JCReturn ret = ctx.treeMaker.Return(invokeCont);

        states.get(endState).add(ret);
    }

    /**
     * Creates a new class declaration holding the state methods and fields for parameters and local variables.
     * @return the generated class declaration
     */
    private JCClassDecl createClassDeclaration() {
        long flags = Flags.PRIVATE | Flags.FINAL;

        /*
         * If the original method was static then the generated class must be a nested static class too. The same
         * applied if the transformed method was located in an interface. In that case we have a default method which
         * is implicitly static.
         */
        if (isStaticContext) {
            flags |= Flags.STATIC;
        }

        final JCModifiers mods = ctx.treeMaker.Modifiers(flags);

        final ListBuffer<JCTree> defs = new ListBuffer<>();

        defs.addAll(fields.values());

        defs.addAll(classDefs);

        defs.add(createStreamMethod());

        for (Map.Entry<Integer, java.util.List<JCStatement>> state : states.entrySet()) {
            defs.add(stateIntoMethod(state.getKey(), state.getValue()));
        }

        rewriteOriginalMethod();

        return ctx.treeMaker.ClassDef(mods, ctx.name(className),
            originalMethod.getTypeParameters(), null, List.nil(), defs.toList());
    }

    /**
     * Replaces the original implementation of the generator method. Removes the original code and instantiates the generator class.
     * Instantiation is followed by passing the parameter values to the class and then creating a stream.
     *
     * <p> So the generated code looks somehow like this: </p>
     * <pre>
     * {@code
     *   $GeneratorImplx $generator = new $GeneratorImplx();
     *   // set fields to parameter values
     *   return $generator.stream();
     * }
     * </pre>
     */
    private void rewriteOriginalMethod() {
        final ListBuffer<JCExpression> typeParams = new ListBuffer<>();

        for (JCTypeParameter param : originalMethod.getTypeParameters()) {
            typeParams.add(ctx.treeMaker.Ident(param.getName()));
        }

        final Name generatorName = ctx.name(GENERATOR_VARIABLE);

        final JCIdent generator = ctx.treeMaker.Ident(generatorName);

        final JCTypeApply clazz =
                ctx.treeMaker.TypeApply(ctx.treeMaker.Ident(ctx.name(className)), typeParams.toList());

        final JCNewClass instantiation = ctx.treeMaker.NewClass(null,
            List.nil(),
            typeParams.length() > 0 ? clazz : ctx.treeMaker.Ident(ctx.name(className)),
            List.nil(),
            null);

        final ListBuffer<JCStatement> stats = new ListBuffer<>();

        final JCStatement generatorAssign =
            ctx.treeMaker.VarDef(ctx.treeMaker.Modifiers(NO_MODIFIERS),
                    generatorName,
                    typeParams.length() > 0 ? clazz : ctx.treeMaker.Ident(ctx.name(className)),
                    instantiation);

        stats.add(generatorAssign);

        if (!isStaticContext) {
            final JCFieldAccess selfFieldAccess =
                    ctx.treeMaker.Select(ctx.treeMaker.Ident(generatorName), ctx.name(this.selfName));

            final JCExpression selfAssign =
                    ctx.treeMaker.Assign(selfFieldAccess, ctx.treeMaker.Ident(ctx.name("this")));

            stats.add(ctx.treeMaker.Exec(selfAssign));
        }

        for (JCVariableDecl param : originalMethod.getParameters()) {
            final JCFieldAccess fieldAccess =
                ctx.treeMaker.Select(ctx.treeMaker.Ident(generatorName), param.getName());

            final JCAssign assign = ctx.treeMaker.Assign(fieldAccess, ctx.treeMaker.Ident(param.sym));

            stats.add(ctx.treeMaker.Exec(assign));
        }

        final JCFieldAccess methodAccess =
            ctx.treeMaker.Select(generator, ctx.name(Identifiers.STREAM_METHOD));

        final JCMethodInvocation invocation =
            ctx.treeMaker.App(methodAccess.setType(originalMethod.getReturnType().type), List.nil());

        stats.add(ctx.treeMaker.Return(invocation));

        originalMethod.body.stats = stats.toList();
    }

    /**
     * Converts a state which is actually just a list of statements into a method declaration.
     * @param index the index of the state
     * @param statements the statements that make up the state
     * @return a new method declaration consisting of the state's statements
     */
    private JCMethodDecl stateIntoMethod(int index, java.util.List<JCStatement> statements) {
        final JCIdent jieldPackage =
                ctx.treeMaker.Ident(ctx.names.fromString(Identifiers.JIELD));

        final JCFieldAccess runtimeAccess =
                ctx.treeMaker.Select(jieldPackage, ctx.names.fromString(Identifiers.RUNTIME));

        final JCFieldAccess bounceAccess =
                ctx.treeMaker.Select(runtimeAccess, ctx.names.fromString(Identifiers.BOUNCE));

        final JCExpression returnType =
            ctx.treeMaker.TypeApply(bounceAccess, List.of(generatedType));

        /*
         * Black magic and witchcraft ahead:
         *
         * Create type symbol and class type so that we can trick javac into thinking
         * that this is properly typed code.
         *
         * There are some suspicious nulls, but they cause no harm.
         */
        final Symbol.PackageSymbol jieldPkgSymbol =
                new Symbol.PackageSymbol(ctx.names.fromString(Identifiers.JIELD), null);

        final Symbol.PackageSymbol runtimePkgSymbol =
                new Symbol.PackageSymbol(ctx.names.fromString(Identifiers.RUNTIME), jieldPkgSymbol);

        final Symbol.TypeSymbol generatorStateSymbol =
            new ClassSymbol(NO_MODIFIERS, ctx.name(Identifiers.GENERATOR_STATE), runtimePkgSymbol);

        final Type.ClassType generatorStateType =
            new Type.ClassType(Type.noType, List.of(generatedType.type), generatorStateSymbol);

        final JCVariableDecl contParam =
            ctx.treeMaker.VarDef(new Symbol.VarSymbol(Flags.PARAMETER,
                ctx.name(CONTINUATION_PARAM), generatorStateType, null), null);

        return ctx.treeMaker.MethodDef(
            ctx.treeMaker.Modifiers(Flags.PRIVATE),
            methodName(index),
            returnType,
            List.nil(),
            List.of(contParam),
            List.nil(),
            ctx.treeMaker.Block(NO_MODIFIERS, List.from(statements)),
            null);
    }

    /**
     * Generates the nested the stream() method of the nested class. This method produces the actual stream.
     *
     * <p> The generated implementation looks somehow like this:
     * <pre>
     * {@code
     *   return BaseGenerator.startingAt(this::<>$0).stream();
     * }
     * </pre>
     * </p>
     * @return the method declaration of the stream method
     */
    private JCMethodDecl createStreamMethod() {
        final JCExpression startStateRef =
            ctx.treeMaker
                .Reference(MemberReferenceTree.ReferenceMode.INVOKE, methodName(0), ctx.treeMaker.Ident(ctx.names._this), List.nil());

        final JCIdent jieldPackage =
                ctx.treeMaker.Ident(ctx.names.fromString(Identifiers.JIELD));

        final JCFieldAccess runtimeAccess =
                ctx.treeMaker.Select(jieldPackage, ctx.names.fromString(Identifiers.RUNTIME));

        final JCFieldAccess baseGenAccess =
                ctx.treeMaker.Select(runtimeAccess, ctx.names.fromString(Identifiers.BASE_GENERATOR));

        final JCExpression selectStartingAt =
            ctx.treeMaker.Select(baseGenAccess, ctx.name(Identifiers.STARTING_AT_METHOD));

        final JCExpression selectStream =
            ctx.treeMaker.Select(ctx.treeMaker.App(selectStartingAt.setType(Type.noType), List.of(startStateRef)),
                ctx.name(Identifiers.STREAM_METHOD));

        final JCMethodInvocation invokeStream =
            ctx.treeMaker.App(selectStream.setType(originalMethod.getReturnType().type));

        final JCReturn ret = ctx.treeMaker.Return(invokeStream);

        return ctx.treeMaker.MethodDef(ctx.treeMaker.Modifiers(Flags.PRIVATE),
            ctx.name(Identifiers.STREAM_METHOD),
            originalMethod.restype,
            List.nil(),
            List.nil(),
            List.nil(),
            ctx.treeMaker.Block(NO_MODIFIERS, List.of(ret)), null);
    }

    /**
     * Generates class fields for the parameters of the original method.
     */
    private void generateParameterFields() {
        for (JCVariableDecl declaration : originalMethod.getParameters()) {
            JCVariableDecl decl = ctx.treeMaker.VarDef(ctx.treeMaker.Modifiers(Flags.PUBLIC),
                declaration.getName(),
                declaration.vartype,
                null);

            fields.put(decl.getName().toString(), decl);
        }
    }

    /*
     * Handles some kind of visitor logic. Determines the specific type of the passed statement
     * and calls the appropriate handler. Should be replaced with a more elegant solution.
     */
    private void transformStatement(JCStatement statement, int current, Continuation cont) {
        if (statement instanceof JCBlock) {
            transformBlock((JCBlock) statement, current, cont);
        } else if (statement instanceof JCVariableDecl) {
            return; // handled by block and case
        } else if (statement instanceof JCReturn) {
            transformYield((JCReturn) statement, current, cont);
        } else if (statement instanceof JCForLoop) {
            transformForLoop((JCForLoop) statement, current, cont);
        } else if (statement instanceof JCWhileLoop) {
            transformWhileLoop((JCWhileLoop) statement, current, cont);
        } else if (statement instanceof JCDoWhileLoop) {
            transformDoWhileLoop((JCDoWhileLoop) statement, current, cont);
        } else if (statement instanceof JCIf) {
            transformIf((JCIf) statement, current, cont);
        } else if (statement instanceof JCSwitch) {
            transformSwitch((JCSwitch) statement, current, cont);
        } else if (statement instanceof JCBreak) {
            transformBreak((JCBreak) statement, current, cont);
        } else if (statement instanceof JCContinue) {
            transformContinue((JCContinue) statement, current, cont);
        } else if (statement instanceof JCLabeledStatement) {
            transformLabeledStatement((JCLabeledStatement) statement, current, cont);
        } else if (statement instanceof JCEnhancedForLoop) {
            transformEnhancedForLoop((JCEnhancedForLoop) statement, current, cont);
        } else if (statement instanceof JCClassDecl) {
            RenamingVisitor.visit(statement, cont, ctx.names);

            classDefs.add(statement);
        } else {
            transformNoop(statement, current, cont);
        }
    }

    private void transformSwitch(JCSwitch statement, int current, Continuation cont) {
        Continuation c = cont;

        final int dispatcherState = newState();

        states.get(current).add(yield(dispatcherState, Optional.empty()));

        RenamingVisitor.visit(statement.selector, c, ctx.names);

        final Map<JCCase, Integer> caseStates = new HashMap<>();

        final Map<JCCase, JCCase> caseMapping = new HashMap<>();

        boolean hasDefaultCase = false;

        for (JCCase jcCase : statement.cases) {
            if (jcCase.pat == null) {
                hasDefaultCase = true;
            }

            caseStates.put(jcCase, newState());

            final JCStatement yieldToCase = yield(caseStates.get(jcCase), Optional.empty());

            final JCCase dispatcherCase = ctx.treeMaker.Case(jcCase.pat, List.of(yieldToCase));

            caseMapping.put(jcCase, dispatcherCase);
        }

        final JCSwitch dispatcherSwitch =
            ctx.treeMaker.Switch(statement.selector, List.from(caseMapping.values()));

        states.get(dispatcherState).add(dispatcherSwitch);

        if (!hasDefaultCase) {
            states.get(dispatcherState).add(yield(cont.getNextCont(), Optional.empty()));
        }

        for (int i = 0; i < statement.cases.length(); ++i) {
            Continuation cnt = c.breakCont(NO_LABEL, c.getNextCont()).clearLabels();

            if (i < statement.cases.length() - 1) {
                cnt = cnt.nextCont(caseStates.get(statement.cases.get(i + 1)));
            }

            transformCase(statement.cases.get(i), caseStates.get(statement.cases.get(i)), cnt);
        }
    }

    private void transformCase(JCCase jcCase, int current, Continuation cont) {
        Continuation c = cont;

        /*
         * Create an empty continuation state for child statements.
         */
        int childContinuation = newState();

        /*
         * Before looping over the block, the current state is empty, so it can
         * be passed to a child statement.
         */
        int childCurrent = current;

        for (JCStatement statement : jcCase.getStatements()) {
            if (statement instanceof JCVariableDecl) {
                c = addVariableAsField((JCVariableDecl) statement, c);

                final int cc = childCurrent;

                convertVariableDeclarationToAssignment((JCVariableDecl) statement, c)
                    .ifPresent(s -> states.get(cc).add(s));

                RenamingVisitor.visit(statement, c, ctx.names);
            } else {
                if (!ModifiesControlFlowVisitor.modifiesControlFlow(statement)) {
                    transformNoop(statement, childCurrent, c.nextCont(childContinuation).label(NO_LABEL));
                } else {
                    transformStatement(statement, childCurrent, c.nextCont(childContinuation).label(NO_LABEL));
                }
            }

            /*
             * We gave the child statement a state and it exhausted it with a yield.
             *
             * Thus a the continuation becomes the new current state and a new state will
             * be created as the continuation.
             */
            if (isStateReturns(childCurrent)) {
                childCurrent = childContinuation;

                childContinuation = newState();
            }
        }

        if (!isStateReturns(childCurrent)) {
            states.get(childCurrent).add(yield(c.getNextCont(), Optional.empty()));
        }

        /*
         * Connect the inner flow to the flow we've received as the continuation.
         */
        states.get(childContinuation).add(yield(c.getNextCont(), Optional.empty()));
    }

    private void hasDefaultCaseWithReturn(JCSwitch statement) {
        for (JCCase jcCase : statement.cases) {
            if (jcCase.pat == null) {

            }
        }
    }

    private void transformEnhancedForLoop(JCEnhancedForLoop loop, int current, Continuation cont) {
        Continuation c = cont;

        final int initState = newState();

        states.get(current).add(yield(initState, Optional.empty()));

        RenamingVisitor.visit(loop.getExpression(), c, ctx.names);

        final java.util.List<JCStatement> initStatements = states.get(initState);

        final JCIdent javaPackage =
                ctx.treeMaker.Ident(ctx.names.fromString(Identifiers.JAVA));

        final JCFieldAccess utilAccess =
                ctx.treeMaker.Select(javaPackage, ctx.names.fromString(Identifiers.UTIL));

        final JCFieldAccess iteratorAccess =
                ctx.treeMaker.Select(utilAccess, ctx.names.fromString(Identifiers.ITERATOR));

        final JCExpression iteratorType;

        if (loop.var.getType() instanceof JCPrimitiveTypeTree) {
            iteratorType =
                    ctx.treeMaker.Ident(ctx.name(primitiveToObject(loop.var.getType().toString())));
        } else {
            iteratorType = (JCExpression) loop.var.getType();
        }

        final JCTypeApply parameterizedIterator =
                ctx.treeMaker.TypeApply(iteratorAccess, List.of(iteratorType));

        final JCIdent jieldPackage =
                ctx.treeMaker.Ident(ctx.names.fromString(Identifiers.JIELD));

        final JCFieldAccess runtimeAccess =
                ctx.treeMaker.Select(jieldPackage, ctx.names.fromString(Identifiers.RUNTIME));

        final JCFieldAccess cpsUtilAccess =
                ctx.treeMaker.Select(runtimeAccess, ctx.names.fromString(Identifiers.CPS_UTIL));

        final JCFieldAccess iteratorMethodAccess =
                ctx.treeMaker.Select(cpsUtilAccess, ctx.names.fromString(Identifiers.ITERATOR_METHOD));

        final JCMethodInvocation iteratorInvocation =
                ctx.treeMaker.App(iteratorMethodAccess.setType(Type.noType),
                        List.of(loop.getExpression()));

        final JCVariableDecl iteratorDecl =
            ctx.treeMaker.VarDef(ctx.treeMaker.Modifiers(NO_MODIFIERS),
                    ctx.name("iterator"),
                    parameterizedIterator,
                    iteratorInvocation);

        c = addVariableAsField(iteratorDecl, c);

        convertVariableDeclarationToAssignment(iteratorDecl, c)
                .ifPresent(initStatements::add);

        c = addVariableAsField(loop.getVariable(), c);

        final int conditionState = newState();

        initStatements.add(yield(conditionState, Optional.empty()));

        /*
         * Place the condition into a separate method.
         */

        final java.util.List<JCStatement> condStatements = states.get(conditionState);

        final ListBuffer<JCStatement> ifBody = new ListBuffer<>();

        final int bodyState = newState();

        final JCIdent iteratorId =
                ctx.treeMaker.Ident(ctx.name(c.nameOf("iterator")));

        final JCFieldAccess nextMethodAccess =
                ctx.treeMaker.Select(iteratorId, ctx.name(Identifiers.NEXT_METHOD));

        final JCMethodInvocation nextMethodInvocation =
                ctx.treeMaker.App(nextMethodAccess.setType(Type.noType));

        final JCAssign assignNext =
                ctx.treeMaker.Assign(ctx.treeMaker.Ident(ctx.name(c.nameOf(loop.var.name.toString()))), nextMethodInvocation);

        ifBody.add(ctx.treeMaker.Exec(assignNext));

        ifBody.add(yield(bodyState, Optional.empty()));

        final JCBlock ifBlock = ctx.treeMaker.Block(NO_MODIFIERS, ifBody.toList());

        final JCFieldAccess hasNextAccess =
                ctx.treeMaker.Select(iteratorId, ctx.name(Identifiers.HAS_NEXT_METHOD));

        final JCExpression cond = ctx.treeMaker.App(hasNextAccess.setType(Type.noType));

        final JCIf conditional = ctx.treeMaker.If(cond, ifBlock, null);

        condStatements.add(conditional);

        condStatements.add(yield(c.getNextCont(), Optional.empty()));

        c = c.nextCont(conditionState)
                .breakCont(NO_LABEL, cont.getNextCont())
                .continueCont(NO_LABEL, conditionState);

        for (String label : c.getLabels()) {
            c = c.continueCont(label, conditionState);
        }

        transformStatement(loop.getStatement(), bodyState, c.clearLabels());
    }

    private void transformNoop(JCStatement statement, int current, Continuation cont) {
        RenamingVisitor.visit(statement, cont, ctx.names);

        states.get(current).add(statement);
    }

    private void transformContinue(JCContinue statement, int current, Continuation cont) {
        if (statement.getLabel() != null) {
            states.get(current).add(yield(cont.getContinueCont(statement.label.toString()), Optional.empty()));
        } else {
            states.get(current).add(yield(cont.getContinueCont(null), Optional.empty()));
        }
    }

    private void transformLabeledStatement(JCLabeledStatement statement, int current, Continuation cont) {
        String label = statement.getLabel().toString();

        transformStatement(statement.getStatement(), current, cont.label(label).breakCont(label, cont.getNextCont()));
    }

    private void transformBreak(JCBreak statement, int current, Continuation cont) {
        if (statement.getLabel() != null) {
            states.get(current).add(yield(cont.getBreakCont(statement.label.toString()), Optional.empty()));
        } else {
            states.get(current).add(yield(cont.getBreakCont(null), Optional.empty()));
        }
    }

    /*
     * Transforms a for loop into recursion (that's not that obvious) and then into separate
     * methods so that it can be used in CPS.
     */
    private void transformForLoop(JCForLoop loop, int current, Continuation cont) {
        /*
         * Going to be reassigned a few times because of break/continue continuations and
         * renamings.
         */
        Continuation c = cont;

        /*
         * Close the "current" state. By closing the current state we can ensure that we have
         * a completely empty and fresh method. This is not necessary but is a good practice.
         */
        final int initState = newState();

        states.get(current).add(yield(initState, Optional.empty()));

        /*
         * Place the initializer which will be run first into a separate state.
         *
         * This way we can be sure that the initializer is going to be called only once.
         */
        final java.util.List<JCStatement> initStatements = states.get(initState);

        for (JCStatement statement : loop.getInitializer()) {
            if (!(statement instanceof JCVariableDecl)) {
                initStatements.add(statement);
            } else {
                c = addVariableAsField((JCVariableDecl) statement, c);

                convertVariableDeclarationToAssignment((JCVariableDecl) statement, c)
                    .ifPresent(initStatements::add);
            }
        }

        final int conditionState = newState();

        initStatements.add(yield(conditionState, Optional.empty()));

        /*
         * Place the condition into a separate method.
         */
        final java.util.List<JCStatement> condStatements = states.get(conditionState);

        final ListBuffer<JCStatement> ifBody = new ListBuffer<>();

        final int bodyState = newState();

        ifBody.add(yield(bodyState, Optional.empty()));

        final JCBlock ifBlock = ctx.treeMaker.Block(NO_MODIFIERS, ifBody.toList());

        final JCExpression cond;

        if (loop.getCondition() != null) {
            cond = loop.getCondition();

            RenamingVisitor.visit(cond, c, ctx.names);
        } else {
            cond = ctx.treeMaker.Literal(Boolean.TRUE);
        }

        final JCIf conditional = ctx.treeMaker.If(cond, ifBlock, null);

        condStatements.add(conditional);

        final int updateState = newState();

        condStatements.add(yield(c.getNextCont(), Optional.empty()));

        /*
         * Place the original update stuff into the update state which
         * will loop back to the condition state through recursion.
         */
        final java.util.List<JCStatement> updateStatements = states.get(updateState);

        for (JCExpressionStatement expressionStatement : loop.getUpdate()) {
            updateStatements.add(expressionStatement);

            RenamingVisitor.visit(expressionStatement, c, ctx.names);
        }


        updateStatements.add(yield(conditionState, Optional.empty()));

        /*
         * Everything's ready, we can let someone else transform the body.
         *
         * It's important to note that the continuation of the created states
         * will be the update state.
         */
        c = c.nextCont(updateState)
                .breakCont(NO_LABEL, c.getNextCont())
                .continueCont(NO_LABEL, updateState);

        for (String label : c.getLabels()) {
            c = c.continueCont(label, updateState);
        }

        transformStatement(loop.getStatement(), bodyState, c.clearLabels());
    }

    private void transformWhileLoop(JCWhileLoop loop, int current, Continuation cont) {
        /*
         * Close the "current" state.
         */
        final int condState = newState();

        states.get(current).add(yield(condState, Optional.empty()));

        final java.util.List<JCStatement> condStatements = states.get(condState);

        final ListBuffer<JCStatement> ifBody = new ListBuffer<>();

        final int bodyState = newState();

        ifBody.add(yield(bodyState, Optional.empty()));

        final JCBlock ifBlock = ctx.treeMaker.Block(NO_MODIFIERS, ifBody.toList());

        final JCExpression cond;

        if (loop.getCondition() != null) {
            cond = loop.getCondition();

            RenamingVisitor.visit(cond, cont, ctx.names);
        } else {
            cond = ctx.treeMaker.Literal(Boolean.TRUE);
        }

        final JCIf conditional = ctx.treeMaker.If(cond, ifBlock, null);

        condStatements.add(conditional);

        condStatements.add(yield(cont.getNextCont(), Optional.empty()));

        Continuation c = cont.nextCont(condState)
                .breakCont(NO_LABEL, cont.getNextCont())
                .continueCont(NO_LABEL, condState);

        for (String label : c.getLabels()) {
            c = c.continueCont(label, condState);
        }

        transformStatement(loop.getStatement(), bodyState, c.clearLabels());
    }

    private void transformDoWhileLoop(JCDoWhileLoop loop, int current, Continuation cont) {
        /*
         * Close the "current" state.
         */
        final int bodyState = newState();

        states.get(current).add(yield(bodyState, Optional.empty()));

        final int condState = newState();

        final java.util.List<JCStatement> condStatements = states.get(condState);

        final ListBuffer<JCStatement> ifBody = new ListBuffer<>();

        ifBody.add(yield(bodyState, Optional.empty()));

        final JCBlock ifBlock = ctx.treeMaker.Block(NO_MODIFIERS, ifBody.toList());

        final JCExpression cond;

        if (loop.getCondition() != null) {
            cond = loop.getCondition();

            RenamingVisitor.visit(cond, cont, ctx.names);
        } else {
            cond = ctx.treeMaker.Literal(Boolean.TRUE);
        }

        final JCIf conditional = ctx.treeMaker.If(cond, ifBlock, null);

        condStatements.add(conditional);

        condStatements.add(yield(cont.getNextCont(), Optional.empty()));

        Continuation c = cont.nextCont(condState)
                .breakCont(NO_LABEL, cont.getNextCont())
                .continueCont(NO_LABEL, condState);

        for (String label : c.getLabels()) {
            c = c.continueCont(label, condState);
        }

        transformStatement(loop.getStatement(), bodyState, c.clearLabels());
    }

    private boolean hasUnconditionalElse(JCIf statement) {
        if (statement.getElseStatement() == null) {
            return false;
        } else if (!(statement.getElseStatement() instanceof JCIf)){
            return true;
        } else {
            return hasUnconditionalElse((JCIf) statement.getElseStatement());
        }
    }

    private void transformIf(JCIf statement, int current, Continuation cont) {
        final int thenState = newState();

        RenamingVisitor.visit(statement.getCondition(), cont, ctx.names);

        transformStatement(statement.getThenStatement(), thenState, cont.label(NO_LABEL));

        JCStatement elsePart = null;

        int elseState = -1;

        final boolean unconditionalElse = hasUnconditionalElse(statement);

        if (statement.getElseStatement() != null) {
            elseState = newState();

            transformStatement(statement.getElseStatement(), elseState, cont.label(NO_LABEL));

            if (!unconditionalElse) {
                final ListBuffer<JCStatement> elseStatements = new ListBuffer<>();

                elseStatements.add(yield(elseState, Optional.empty()));

                elsePart = ctx.treeMaker.Block(NO_MODIFIERS, elseStatements.toList());
            }
        }

        final ListBuffer<JCStatement> thenStatements = new ListBuffer<>();

        thenStatements.add(yield(thenState, Optional.empty()));

        final JCStatement thenPart = ctx.treeMaker.Block(NO_MODIFIERS, thenStatements.toList());

        final JCIf newIf = ctx.treeMaker.If(statement.getCondition(), thenPart, elsePart);

        states.get(current).add(newIf);

        if (!unconditionalElse) {
            states.get(current).add(yield(cont.getNextCont(), Optional.empty()));
        } else {
            states.get(current).add(yield(elseState, Optional.empty()));
        }
    }

    /*
     * Transform a block by passing all of its statements to other transformers.
     */
    private void transformBlock(JCBlock block, int current, Continuation cont) {
        Continuation c = cont;

        /*
         * Create an empty continuation state for child statements.
         */
        int childContinuation = newState();

        /*
         * Before looping over the block, the current state is empty, so it can
         * be passed to a child statement.
         */
        int childCurrent = current;

        for (JCStatement statement : block.getStatements()) {
            if (statement instanceof JCVariableDecl) {
                c = addVariableAsField((JCVariableDecl) statement, c);

                final int cc = childCurrent;

                convertVariableDeclarationToAssignment((JCVariableDecl) statement, c)
                    .ifPresent(s -> states.get(cc).add(s));

                RenamingVisitor.visit(statement, c, ctx.names);
            } else {
                if (!ModifiesControlFlowVisitor.modifiesControlFlow(statement)) {
                    transformNoop(statement, childCurrent, c.nextCont(childContinuation).label(NO_LABEL));
                } else {
                    transformStatement(statement, childCurrent, c.nextCont(childContinuation).label(NO_LABEL));
                }
            }

            /*
             * We gave the child statement a state and it exhausted it with a yield.
             *
             * Thus a the continuation becomes the new current state and a new state will
             * be created as the continuation.
             */
            if (isStateReturns(childCurrent)) {
                childCurrent = childContinuation;

                childContinuation = newState();
            }
        }

        if (!isStateReturns(childCurrent)) {
            states.get(childCurrent).add(yield(c.getNextCont(), Optional.empty()));
        }

        /*
         * Connect the inner flow to the flow we've received as the continuation.
         */
        states.get(childContinuation).add(yield(c.getNextCont(), Optional.empty()));
    }

    private void transformYield(JCReturn ret, int current, Continuation cont) {
        RenamingVisitor.visit(ret, cont, ctx.names);

        states.get(current).add(yield(cont.getNextCont(), Optional.of(ret.getExpression())));
    }

    /*
     * Yield generates a return statement with a new {@code Bounce} instance to the next method.
     * Optionally an expression can be passed which will be the return type of the generator after
     * evaluated.
     */
    private JCStatement yield(int next, Optional<JCExpression> returnExpr) {
        final JCMethodInvocation contInvocation =
            ctx.treeMaker.App(ctx.treeMaker.Ident(methodName(next)).setType(Type.noType),
                List.of(ctx.treeMaker.Ident(ctx.name(CONTINUATION_PARAM))));

        final JCLambda lambda = ctx.treeMaker.Lambda(List.nil(), contInvocation);

        final List<JCExpression> bounceParams = returnExpr.isPresent() ? List.of(lambda, returnExpr.get()) : List.of(lambda);

        final JCIdent jieldPackage =
                ctx.treeMaker.Ident(ctx.names.fromString(Identifiers.JIELD));

        final JCFieldAccess runtimeAccess =
                ctx.treeMaker.Select(jieldPackage, ctx.names.fromString(Identifiers.RUNTIME));

        final JCFieldAccess bounceAccess =
                ctx.treeMaker.Select(runtimeAccess, ctx.names.fromString(Identifiers.BOUNCE));

        final JCFieldAccess contAccess =
                ctx.treeMaker.Select(bounceAccess, ctx.names.fromString(Identifiers.CONT_METHOD));

        final JCMethodInvocation bounceInvocation =
            ctx.treeMaker.App(contAccess.setType(Type.noType),
                bounceParams);

        return ctx.treeMaker.Return(bounceInvocation);
    }

    /**
     * Adds a new empty state to the map of states and returns its index.
     * @return the index of the newly created state.
     */
    private int newState() {
        ++maxState;

        states.put(maxState, new ArrayList<>());

        return maxState;
    }

    /**
     * Returns the {@code Name} instance corresponding to the method that represents the state with the passed index.
     * @param state the state
     * @return the {@code Name} of the method
     */
    private Name methodName(int state) {
        return ctx.name(methodNamePrefix + Integer.toString(state));
    }

    /**
     * Checks whether the state has any statements.
     * @param state the state to be checked
     * @return {@code true} if the state currently has no statements, {@code false} otherwise
     */
    private boolean isStateEmpty(int state) {
        return states.get(state).isEmpty();
    }

    /**
     * Checks if the specified state ends with a return statement. In that case the state should be no longer used.
     * @param state the state to be checked
     * @return {@code true} if the state ends with a return statement, {@code false} otherwise
     */
    private boolean isStateReturns(int state) {
        if (isStateEmpty(state)) {
            return false;
        }

        final java.util.List<JCStatement> statements = states.get(state);

        final JCStatement last = statements.get(statements.size() - 1);

        return (last instanceof JCBreak) || (last instanceof JCReturn);
    }

    /*
     * Creates a field corresponding to the passed local variable declaration.
     */
    private Continuation addVariableAsField(JCVariableDecl localVariableDecl, Continuation cont) {
        final JCModifiers mods = ctx.treeMaker.Modifiers(Flags.PRIVATE);

        String original = localVariableDecl.getName().toString();

        final Name name = ctx.names.fromString(generateUnusedIdentifier(original));

        final JCExpression variableType = localVariableDecl.vartype;

        fields.put(name.toString(), ctx.treeMaker.VarDef(mods, name, variableType, null));

        return cont.rename(localVariableDecl.getName().toString(), name.toString());
    }

    /**
     * Returns an assignment that comprises the declared variable in the specified declaration and the initializer.
     * @param declaration the declaration
     * @return a new assignment statement
     */
    private Optional<JCStatement> convertVariableDeclarationToAssignment(JCVariableDecl declaration, Continuation cont) {
        if (declaration.getInitializer() == null) {
            return Optional.empty();
        }

        final Name name = ctx.names.fromString(cont.nameOf(declaration.getName().toString()));

        final JCExpression nameExpression = ctx.treeMaker.Ident(name);

        return Optional.of(ctx.treeMaker.Exec(ctx.treeMaker.Assign(nameExpression, declaration.getInitializer())));
    }

    private String generateUnusedIdentifier(String base) {
        String result = base;

        while (fields.containsKey(result)) {
            result += "$";
        }

        return result;
    }

    private String primitiveToObject(String primitive) {
        switch (primitive) {
            case "byte": return "Byte";
            case "short": return "Short";
            case "int": return "Integer";
            case "long": return "Long";
            case "float": return "Float";
            case "double": return "Double";
            case "boolean": return "Boolean";
            case "char": return "Character";
            default: return primitive;
        }
    }
}
