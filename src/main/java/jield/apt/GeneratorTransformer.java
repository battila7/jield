package jield.apt;

import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

/*
 * Class that does the heavy-lifting of the transformation process. Takes a method and its enclosing class
  * and creates a nested generator class and rewrites the original method.
 */
final class GeneratorTransformer {
    private static final String CLASS_NAME_PREFIX = "$GeneratorImpl";

    private final JCClassDecl enclosingClass;

    private final JCMethodDecl originalMethod;

    private final ProcessingContext ctx;

    private final String className;



    GeneratorTransformer(JCClassDecl enclosingClass, JCMethodDecl originalMethod, ProcessingContext ctx, int index) {
        this.enclosingClass = enclosingClass;

        this.originalMethod = originalMethod;

        this.ctx = ctx;

        this.className = CLASS_NAME_PREFIX + Integer.toString(index);
    }

    /**
     * Performs the transformation on the method passed to the constructor. Destructive in the sense that the original
     * implementation if the method will not be preserved but rewritten to appropriate calls to the new nested
     * generator class.
     * @return a new class that contains the generator logic
     */
    JCClassDecl transform() {
      return null;
    }
}
