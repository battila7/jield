package jield.apt;

import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Names;

final class RenamingVisitor extends TreeScanner {
    private final JCTree tree;

    private Continuation cont;

    private final Names names;

    static void visit(JCTree tree, Continuation cont, Names names) {
        new RenamingVisitor(tree, cont, names).start();
    }

    private RenamingVisitor(JCTree tree, Continuation cont, Names names) {
        this.tree = tree;

        this.cont = cont;

        this.names = names;
    }

    private void start() {
        tree.accept(this);
    }

    @Override
    public void visitIdent(JCIdent jcIdent) {
        jcIdent.name = names.fromString(cont.nameOf(jcIdent.name.toString()));

        super.visitIdent(jcIdent);
    }

    @Override
    public void visitLambda(JCLambda jcLambda) {
        Continuation c = cont;

        for (VariableTree varTree : jcLambda.getParameters()) {
            c = c.rename(varTree.getName().toString(), varTree.getName().toString());
        }

        visit(jcLambda.body, c, names);
    }

    @Override
    public void visitSelect(JCFieldAccess jcFieldAccess) {
        /*
         * Do nothing for now.
         *
         * TODO: this
         */
    }

    @Override
    public void visitClassDef(JCClassDecl jcClassDecl) {
        Continuation c = cont;

        for (JCTree s : jcClassDecl.defs) {
            if (s instanceof JCVariableDecl) {
                JCVariableDecl var = (JCVariableDecl) s;

                c = c.rename(var.getName().toString(), var.getName().toString());
            }
        }

        for (JCTree s : jcClassDecl.defs) {
            if (!(s instanceof JCVariableDecl)) {
                RenamingVisitor.visit(s, c, names);
            }
        }
    }

    @Override
    public void visitMethodDef(JCMethodDecl jcMethodDecl) {
        Continuation c = cont;

        for (JCVariableDecl varDecl : jcMethodDecl.getParameters()) {
            c = c.rename(varDecl.getName().toString(), varDecl.getName().toString());
        }

        visit(jcMethodDecl.getBody(), c, names);
    }

    @Override
    public void visitVarDef(JCVariableDecl jcVariableDecl) {
        cont = cont.rename(jcVariableDecl.getName().toString(), jcVariableDecl.getName().toString());

        super.visitVarDef(jcVariableDecl);
    }
}
