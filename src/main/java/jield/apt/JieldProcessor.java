package jield.apt;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@SupportedAnnotationTypes("*")
public final class JieldProcessor extends AbstractProcessor {
    /**
     * Provides access to the compiler's nametable. Will be used to craft new names for new identifiers and
     * get names of the existing ones.
     */
    private Names names;

    /**
     * Can be used to create new trees in the AST. We will make use of this class as a factory for our generated
     * methods.
     */
    private TreeMaker treeMaker;

    /**
     * Enables us to get the compilation unit of an {@link javax.lang.model.element.Element}. This is necessary
     * because we want to transform compilation units not just classes. For example we need to add imports.
     */
    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        /*
         * The ProcessingEnvironment interface lacks a getContext method, so we must get creative and cast it to
         * a JavacProcessingEnvironment thus destroy the abstraction.
         */
        final Context context = ((JavacProcessingEnvironment) processingEnv).getContext();

        this.names = Names.instance(context);

        this.treeMaker = TreeMaker.instance(context);

        this.trees = Trees.instance(processingEnv);

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        /*
         * Acquire compilation units that should be checked for the presence of the @Generator annotation in this round.
         */
        roundEnv.getRootElements()
                .stream()
                .map(this::resolveCompilationUnit)
                .flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty));

        /*
         * Enable subsequent processing. Doesn't really matter though because no annotation are claimed
         * by this processor.
         */
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    /**
     * Gets the compilation unit in which the passed element is located.
     * @param element the element which need its compilation unit resolved
     * @return an empty {@code Optional} if the compilation unit could not be resolved, otherwise
     * an {@code Optional} with the corresponding compilation unit
     */
    private Optional<JCCompilationUnit> resolveCompilationUnit(Element element) {
        final Optional<TreePath> path = Optional.ofNullable(trees.getPath(element));

        return path.map(p -> (JCCompilationUnit) p.getCompilationUnit());
    }
}
