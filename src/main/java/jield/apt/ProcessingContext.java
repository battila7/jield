package jield.apt;

import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.Messager;
import java.util.Objects;

/**
 * Groups instances of classes used during the transformation process together. Since these objects are required
 * by all classes that take part in the processing, it's easier to pass around this class rather than passing the
 * instances separately.
 */
final class ProcessingContext {
    /**
     * Provides access to the compiler's nametable. Will be used to craft new names for new identifiers and
     * get names of the existing ones.
     */
    final Names names;

    /**
     * Can be used to create new trees in the AST. We will make use of this class as a factory for our generated
     * methods.
     */
    final TreeMaker treeMaker;

    /**
     * Using a messager instance, our classes can display messages in a standard way rather than using
     * {@code System.out.println} or some other logging solution.
     */
    final Messager messager;

    ProcessingContext(Names names, TreeMaker treeMaker, Messager messager) {
        this.names = Objects.requireNonNull(names);

        this.treeMaker = Objects.requireNonNull(treeMaker);

        this.messager = Objects.requireNonNull(messager);
    }

    Name name(String s) {
        return names.fromString(s);
    }
}
