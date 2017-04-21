package jield.apt;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.util.ListBuffer;

import javax.lang.model.element.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static com.sun.tools.javac.tree.JCTree.*;
import static java.util.stream.Collectors.toList;

/**
 * Class responsible for discovering generator methods in a class declaration and initiating their transformation
 * process.
 */
final class ClassTransformer {
    private final JCClassDecl classDeclaration;

    private final ProcessingContext ctx;

    /**
     * {@code true} if the currently transformed class declaration is an interface.
     */
    private final boolean isInterface;

    /**
     * Counter used to add an index suffix to nested generator classes in order to prevent name clashes.
     */
    private int generatorClassIndex;

    ClassTransformer(JCClassDecl classDeclaration, ProcessingContext ctx) {
        this.classDeclaration = Objects.requireNonNull(classDeclaration);

        this.ctx = Objects.requireNonNull(ctx);

        this.isInterface = Kind.INTERFACE.equals(classDeclaration.getKind());

        this.generatorClassIndex = 0;
    }

    /**
     * Finds generator methods and recursively transforms nested classes.
     *
     * The transformation cannot be performed on classes declared within methods because no type
     * information is present for them at the moment of annotation processing. This is quite surprising.
     */
    void performTransformation() {
        for (JCTree tree : classDeclaration.defs) {
            if (tree instanceof JCClassDecl) {
                final ClassTransformer classTransformer = new ClassTransformer((JCClassDecl) tree, ctx);

                classTransformer.performTransformation();
            }
        }

        for (JCMethodDecl method : findGeneratorMethods()) {
            if (!canBeTransformed(method)) {
                /*
                 * TODO: throw some exception to indicate that this method cannot be transformed
                 */

                return;
            }

            /*
             * Create a new nested class and add it to the definition list of the current class declaration.
             */
            final GeneratorTransformer transformer =
                    new GeneratorTransformer(classDeclaration, method, ctx, generatorClassIndex);

            classDeclaration.defs = classDeclaration.defs.append(transformer.transform());

            removeGeneratorAnnotationFromMethod(method);

            ++generatorClassIndex;
        }
    }

    /**
     * Removes the {@link jield.annotation.Generator} annotation from the specified method declaration. This
     * prevents annotation processing to transform the same method twice. All other annotations will be preserved.
     * @param method the method from which the annotation should be removed
     */
    private void removeGeneratorAnnotationFromMethod(JCMethodDecl method) {
        final ListBuffer<JCAnnotation> keptAnnotations = new ListBuffer<>();

        for (JCAnnotation annotation : method.getModifiers().annotations) {
            if (!isGeneratorAnnotation(annotation)) {
                keptAnnotations.add(annotation);
            }
        }

        method.getModifiers().annotations = keptAnnotations.toList();
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
     * Checks if the method's return type is {@link java.util.stream.Stream} and it has a type parameter.
     * @param method the method to be checked
     * @return whether the method's return type is correct
     */
    private boolean isReturnStream(JCMethodDecl method) {


        final boolean isStream = Stream.class.getName().equals(method.getReturnType().type.tsym.toString());

        return isStream && ((Type.ClassType) method.getReturnType().type).typarams_field.length() == 1;
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

        return modifiers.getAnnotations()
                .stream()
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

        return Identifiers.GENERATOR_ANNOTATION.equals(identifierName);
    }
}
