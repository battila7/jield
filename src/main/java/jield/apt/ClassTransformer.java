package jield.apt;

import com.sun.tools.javac.tree.JCTree.JCClassDecl;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;

import static com.sun.tools.javac.tree.JCTree.*;
import static java.util.stream.Collectors.toList;

/**
 * Class responsible for discovering generator methods in a class declaration and initiating their transformation
 * process.
 */
final class ClassTransformer {
    private static final String GENERATOR = "Generator";

    private final JCClassDecl classDeclaration;

    private final ProcessingContext ctx;

    private final boolean isInterface;

    ClassTransformer(JCClassDecl classDeclaration, ProcessingContext ctx) {
        this.classDeclaration = Objects.requireNonNull(classDeclaration);

        this.ctx = Objects.requireNonNull(ctx);

        this.isInterface = Kind.INTERFACE.equals(classDeclaration.getKind());
    }

    /**
     * Finds generator methods and performs the transformation process on the class declaration.
     * @return {@code true} if at least one generator method was found and transformed, {@code false} otherwise
     */
    boolean performTransformation() {
        boolean isTransformationPerformed = false;

        for (JCMethodDecl method : findGeneratorMethods()) {
            if (!canBeTransformed(method)) {
                // throw some exception that this method cannot be transformed

                return false;
            }

            isTransformationPerformed = true;
        }

        return isTransformationPerformed;
    }

    /**
     * Decides whether the specified generator method can be actually transformed.
     *
     * A method marked with the {@link jield.annotation.Generator} annotation must not be <b>abstract</b> and
     * must have return type of {@link java.util.stream.Stream}. If the method is declared in an interface, then
     * it must be a <b>default</b> method.
     * @param method the method to be checked
     * @return whether the method satisfies the requirements of the transformation
     */
    private boolean canBeTransformed(JCMethodDecl method) {
        boolean eligible = !isAbstract(method);

        eligible &= isReturnStream(method);

        eligible &= !isInterface || isDefault(method);

        return eligible;
    }

    /**
     * Checks if the method is an <b>abstract</b> method.
     * @param method the method to be checked
     * @return {@code true} if the method is <b>abstract</b>, {@code false} otherwise
     */
    private boolean isAbstract(JCMethodDecl method) {
        return method.getModifiers().getFlags().contains(Modifier.ABSTRACT);
    }

    /**
     * Checks if the method is a <b>default</b> method.
     * @param method the method to be checked
     * @return {@code true} if the method is <b>default</b>, {@code false} otherwise
     */
    private boolean isDefault(JCMethodDecl method) {
        return method.getModifiers().getFlags().contains(Modifier.DEFAULT);
    }

    /**
     * Checks if the method's return type is {@link java.util.stream.Stream}.
     * @param method the method to be checked
     * @return whether the method's return type is correct
     */
    private boolean isReturnStream(JCMethodDecl method) {
        /*
         * TODO: Check if the return type is java.util.stream.Stream
         */

        return true;
    }

    /**
     * Discovers methods annotated with the {@link jield.annotation.Generator} annotation.
     * @return a list of methods with the proper annotation present
     */
    private List<JCMethodDecl> findGeneratorMethods() {
        return classDeclaration.getMembers().stream()
                .filter(t -> t instanceof JCMethodDecl)
                .map(t -> (JCMethodDecl) t)
                .filter(this::hasGeneratorAnnotation)
                .collect(toList());
    }

    /**
     * Checks if a method is marked with the {@link jield.annotation.Generator} annotation.
     * @param method the method to be inspected
     * @return {@code true} if the method is a Generator method, {@code false} otherwise
     */
    private boolean hasGeneratorAnnotation(JCMethodDecl method) {
        final JCModifiers modifiers = method.getModifiers();

        return modifiers.getAnnotations().stream()
                .anyMatch(this::isGeneratorAnnotation);
    }

    /**
     * Checks whether the specified annotation is an instance of {@link jield.annotation.Generator}.
     * @param annotation the annotation to be inspected
     * @return {@code true} if the annotation is a {@code Generator} annotation, {@code false} otherwise
     */
    private boolean isGeneratorAnnotation(JCAnnotation annotation) {
        final JCIdent identifier = (JCIdent) annotation.getAnnotationType();

        final String identifierName = identifier.getName().toString();

        return GENERATOR.equals(identifierName);
    }
}
