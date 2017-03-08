package jield.apt;

import com.sun.tools.javac.tree.JCTree.JCClassDecl;

import java.util.Objects;

/**
 * Class responsible for discovering generator methods in a class declaration and initiating their transformation
 * process.
 */
final class ClassTransformer {
    private final JCClassDecl classDeclaration;

    private final ProcessingContext ctx;

    ClassTransformer(JCClassDecl classDeclaration, ProcessingContext ctx) {
        this.classDeclaration = Objects.requireNonNull(classDeclaration);

        this.ctx = Objects.requireNonNull(ctx);
    }

    /**
     * Finds generator methods and performs the transformation process on the class declaration.
     * @return {@code true} if at least one generator method was found and transformed, {@code false} otherwise
     */
    boolean performTransformation() {
        return false;
    }
}
