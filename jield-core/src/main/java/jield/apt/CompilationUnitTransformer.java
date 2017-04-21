package jield.apt;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Class that orchestrates the transformation of compilation units. Compilation units might contain multiple
 * type definitions that can in turn contain multiple generator methods. The type definitions must be transformed
 * separately but their containing compilation unit must be modified too by adding suitable import statements
 * for example.
 *
 * <p>
 * If no type definition containing a generator method found, then the compilation unit will not be modified.
 * </p>
 */
final class CompilationUnitTransformer {
    private final JCCompilationUnit compilationUnit;

    private final ProcessingContext ctx;

    /**
     * Constructs a new instance working on the specified compilation unit.
     * @param compilationUnit the compilation unit to be inspected and possibly transformed
     * @param ctx the context supplying objects required to the transformation
     */
    CompilationUnitTransformer(JCCompilationUnit compilationUnit, ProcessingContext ctx) {
        this.compilationUnit = Objects.requireNonNull(compilationUnit);

        this.ctx = Objects.requireNonNull(ctx);
    }

    /**
     * Performs the transformation of the compilation unit. Does nothing if no generator method found.
     */
    void performTransformation() {
        compilationUnit.getTypeDecls()
                .stream()
                .filter(t -> t instanceof JCClassDecl)
                .map(t -> (JCClassDecl) t)
                .forEach(this::transformClassDeclaration);

        System.out.println(compilationUnit);
    }

    /**
     * Takes a class declared located in the current compilation unit and applies the transformation process to it.
     * @param classDeclaration the class declaration to be inspected and transformed
     */
    private void transformClassDeclaration(JCClassDecl classDeclaration) {
        final ClassTransformer classTransformer = new ClassTransformer(classDeclaration, ctx);

        classTransformer.performTransformation();
    }
}
