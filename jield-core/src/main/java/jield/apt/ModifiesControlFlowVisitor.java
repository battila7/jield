package jield.apt;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;

public class ModifiesControlFlowVisitor extends TreeScanner {
    private boolean hasReturn;

    public static boolean modifiesControlFlow(JCTree tree) {
        final ModifiesControlFlowVisitor visitor = new ModifiesControlFlowVisitor();

        tree.accept(visitor);

        return visitor.hasReturn;
    }

    public ModifiesControlFlowVisitor() {
        this.hasReturn = false;
    }

    @Override
    public void visitReturn(JCTree.JCReturn jcReturn) {
        this.hasReturn = true;
    }

    @Override
    public void visitBreak(JCTree.JCBreak jcBreak) {
        if (jcBreak.label != null) {
            this.hasReturn = true;
        }
    }

    @Override
    public void visitContinue(JCTree.JCContinue jcContinue) {
        if (jcContinue.label != null) {
            this.hasReturn = true;
        }
    }
}
