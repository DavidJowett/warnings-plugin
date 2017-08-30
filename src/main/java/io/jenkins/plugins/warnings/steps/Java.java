package io.jenkins.plugins.warnings.steps;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

import io.jenkins.plugins.analysis.core.steps.IssueParser;

import hudson.Extension;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.warnings.parser.AbstractWarningsParser;
import hudson.plugins.warnings.parser.FileWarningsParser;
import hudson.plugins.warnings.parser.ParserRegistry;

/**
 * FIXME: write comment.
 *
 * @author Ullrich Hafner
 */
@Extension
public class Java extends IssueParser {
    @DataBoundConstructor
    public Java() {
        super("java");
    }

    @Override
    public Collection<FileAnnotation> parse(final File file, final String moduleName) throws InvocationTargetException {
        List<AbstractWarningsParser> parsers = ParserRegistry.getParsers("Java Compiler");

        return new FileWarningsParser(parsers, getDefaultEncoding()).parse(file, moduleName);
    }

    @Extension
    public static final IssueParserDescriptor D = new IssueParserDescriptor(Java.class);
}
