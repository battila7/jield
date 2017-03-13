package jield.apt;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.tools.Diagnostic.Kind.ERROR;

@SupportedAnnotationTypes("*")
public final class JieldProcessor extends AbstractProcessor {
    /**
     * Enables us to get the compilation unit of an {@link javax.lang.model.element.Element}. This is necessary
     * because we want to transform compilation units not just classes. For example we need to add imports.
     */
    private Trees trees;

    private ProcessingContext ctx;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        /*
         * The ProcessingEnvironment interface lacks a getContext method, so we must get creative and cast it to
         * a JavacProcessingEnvironment thus destroy the abstraction.
         */
        final Context context = ((JavacProcessingEnvironment) processingEnv).getContext();

        this.trees = Trees.instance(processingEnv);

        this.ctx = new ProcessingContext(Names.instance(context), TreeMaker.instance(context),
                processingEnv.getMessager());

        super.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        /*
         * Acquire compilation units that should be checked for the presence of the @Generator annotation in this round.
         */
        List<JCCompilationUnit> units =
            roundEnv.getRootElements()
                .stream()
                .map(this::resolveCompilationUnit)
                .flatMap(opt -> opt.map(Stream::of).orElseGet(Stream::empty))
                .collect(Collectors.toList());

        /*
         * Perform transformation on the collected compilation units.
         */
        for (JCCompilationUnit unit : units) {
            transformCompilationUnit(unit);

            /*
            try {
                transformCompilationUnit(unit);
            } catch (Exception e) {
                *//*
                 * TODO: Use exception chaining, so all issues will be reported not just the one that
                 *       was encountered first
                 *//*
                //ctx.messager.printMessage(ERROR, e.getMessage());
            }*/
        }

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
     * Initiates the transformation of the specified compilation unit. Transformation is only carried out if
     * the unit has a method with the {@link jield.annotation.Generator} annotation present.
     * @param compilationUnit the unit to be transformed
     */
    private void transformCompilationUnit(JCCompilationUnit compilationUnit) {
        final JCCompilationUnit unit = Objects.requireNonNull(compilationUnit);

        final CompilationUnitTransformer transformer = new CompilationUnitTransformer(unit, ctx);

        transformer.performTransformation();
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
