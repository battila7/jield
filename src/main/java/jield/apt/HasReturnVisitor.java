package jield.apt;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

public class HasReturnVisitor extends TreeScanner {
    private boolean hasReturn;

    public static boolean hasReturn(JCTree tree) {
        final HasReturnVisitor visitor = new HasReturnVisitor();

        tree.accept(visitor);

        return visitor.hasReturn;
    }

    public HasReturnVisitor() {
        this.hasReturn = false;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        this.hasReturn = true;
    }
}
