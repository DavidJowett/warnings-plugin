package io.jenkins.plugins.warnings.steps;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.analysis.core.FilesParser;
import hudson.plugins.analysis.core.ParserResult;
import hudson.plugins.analysis.util.ModuleDetector;
import hudson.plugins.analysis.util.NullModuleDetector;
import hudson.plugins.analysis.util.PluginLogger;
import hudson.plugins.analysis.util.model.FileAnnotation;
import hudson.plugins.warnings.ConsoleParser;
import hudson.plugins.warnings.ParserConfiguration;
import hudson.plugins.warnings.parser.FileWarningsParser;
import hudson.plugins.warnings.parser.ParserRegistry;

/*
 TODO:

 - remove isMavenBuild from FilesParser
 - do we need the getters?
 */
public class ParseWarningsStep extends Step {
    private String defaultEncoding;
    private boolean shouldDetectModules;
    private List<ParserConfiguration> parserConfigurations = Lists.newArrayList();
    private List<ConsoleParser> consoleParsers = Lists.newArrayList();

    @DataBoundConstructor
    public ParseWarningsStep() {
        // empty constructor required for Stapler
    }

    public ConsoleParser[] getConsoleParsers() {
        return ConsoleParser.filterExisting(consoleParsers);
    }

    /**
     * Sets the names of the parsers for the console log.
     *
     * @param consoleParsers the parsers to use
     */
    @DataBoundSetter
    public void setConsoleParsers(final @CheckForNull ConsoleParser[] consoleParsers) {
        if (consoleParsers != null && consoleParsers.length > 0) {
            this.consoleParsers.addAll(Arrays.asList(consoleParsers));
        }
    }

    public ParserConfiguration[] getParserConfigurations() {
        return ParserConfiguration.filterExisting(parserConfigurations);
    }

    /**
     * Sets the parsers and filename patterns to use.
     *
     * @param parserConfigurations the parser and filename patterns to use
     */
    @DataBoundSetter
    public void setParserConfigurations(final ParserConfiguration[] parserConfigurations) {
        if (parserConfigurations != null && parserConfigurations.length > 0) {
            this.parserConfigurations.addAll(Arrays.asList(parserConfigurations));
        }
    }

    public boolean getShouldDetectModules() {
        return shouldDetectModules;
    }

    /**
     * Enables or disables module scanning. If {@code shouldDetectModules} is set, then the module
     * name is derived by parsing Maven POM or Ant build files.
     *
     * @return shouldDetectModules if set to {@code true} then modules are scanned.
     */
    @DataBoundSetter
    public void setShouldDetectModules(final boolean shouldDetectModules) {
        this.shouldDetectModules = shouldDetectModules;
    }

    @CheckForNull
    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    /**
     * Sets the default encoding used to read files (warnings, source code, etc.).
     *
     * @param defaultEncoding the encoding, e.g. "ISO-8859-1"
     */
    @DataBoundSetter
    public void setDefaultEncoding(final String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    @Override
    public StepExecution start(final StepContext stepContext) throws Exception {
        return new Execution(stepContext, this);
    }

    public static class Execution extends SynchronousNonBlockingStepExecution<ParserResult> {
        private final String defaultEncoding;
        private final boolean shouldDetectModules;
        private final ParserConfiguration[] parserConfigurations;
        private final ConsoleParser[] consoleParsers;

        protected Execution(@Nonnull final StepContext context, final ParseWarningsStep step) {
            super(context);

            defaultEncoding = step.getDefaultEncoding();
            shouldDetectModules = step.getShouldDetectModules();
            consoleParsers = step.getConsoleParsers();
            parserConfigurations = step.getParserConfigurations();

            if (consoleParsers.length == 0 && parserConfigurations.length == 0) {
                throw new IllegalArgumentException(
                        "Error: No warning parsers defined for the step: " + toString());
            }
        }

        @Override
        protected ParserResult run() throws Exception {
            FilePath workspace = getContext().get(FilePath.class);
            TaskListener logger = getContext().get(TaskListener.class);

            PluginLogger pluginLogger = new PluginLogger(logger.getLogger(), "warnings");

            if (workspace != null) {
                ParserResult result = parseFiles(parserConfigurations, workspace, pluginLogger);
                logger.getLogger().append(result.getLogMessages());

                result.addProject(parseConsoleLog(consoleParsers, workspace, pluginLogger));
                logger.getLogger().append(result.getLogMessages());

                return result;
            }
            else {
                logger.error("No workspace found.");
                return new ParserResult();
            }
        }

        /** Maximum number of times that the environment expansion is executed. */
        private static final int RESOLVE_VARIABLES_DEPTH = 10;

        /**
         * Resolve build parameters in the file pattern up to {@link #RESOLVE_VARIABLES_DEPTH} times.
         *
         * @param unexpanded the pattern to expand
         */
        private String expandEnvironmentVariables(final String unexpanded) {
            String expanded = unexpanded;
            try {
                EnvVars environment = getContext().get(EnvVars.class);
                if (environment != null && !environment.isEmpty()) {
                    for (int i = 0; i < RESOLVE_VARIABLES_DEPTH && StringUtils.isNotBlank(expanded); i++) {
                        String old = expanded;
                        expanded = Util.replaceMacro(expanded, environment);
                        if (old.equals(expanded)) {
                            break;
                        }
                    }
                }
            }
            catch (IOException e) {
                // ignore
            }
            catch (InterruptedException e) {
                // ignore
            }
            return expanded;
        }

        private ParserResult parseFiles(final ParserConfiguration[] parserConfigurations, final FilePath workspace,
                final PluginLogger logger) throws IOException, InterruptedException {
            ParserResult result = new ParserResult();
            logger.log("Parsing %d patterns in %s for warnings.", parserConfigurations.length, workspace);
            for (ParserConfiguration configuration : parserConfigurations) {
                String filePattern = expandEnvironmentVariables(configuration.getPattern());
                String parserName = configuration.getParserName();

                logger.log("Parsing warnings in files defined by the pattern '%s' with parser 's'",
                        filePattern, parserName);

                FilesParser parser = new FilesParser("warnings", filePattern,
                        new FileWarningsParser(ParserRegistry.getParsers(parserName), defaultEncoding),
                        shouldDetectModules);
                ParserResult project = workspace.act(parser);
                logger.logLines(project.getLogMessages());

                returnIfCanceled();

                result.addProject(project);
            }
            return result;
        }

        private ParserResult parseConsoleLog(final ConsoleParser[] parsers, final FilePath workspace, final PluginLogger logger)
                throws IOException, InterruptedException {
            ParserResult result = new ParserResult();
            logger.log("Parsing console log with %d parsers.", parsers.length);
            for (ConsoleParser parser : parsers) {
                String parserName = parser.getParserName();

                logger.log("Parsing warnings in console log with parser " + parserName);

                Run run = getContext().get(Run.class);
                Collection<FileAnnotation> warnings = new ParserRegistry(ParserRegistry.getParsers(parserName),
                        defaultEncoding).parse(run.getLogFile());
                if (!workspace.isRemote() && shouldDetectModules) {
                    guessModuleNames(workspace, warnings);
                }

                result.addAnnotations(warnings);
            }
            return result;
        }

        private void guessModuleNames(final FilePath workspace, final Collection<FileAnnotation> warnings) {
            ModuleDetector detector = createModuleDetector(workspace.getRemote());
            for (FileAnnotation annotation : warnings) {
                String module = detector.guessModuleName(annotation.getFileName());
                annotation.setModuleName(module);
            }
        }

        private ModuleDetector createModuleDetector(final String workspace) {
            if (shouldDetectModules) {
                return new ModuleDetector(new File(workspace));
            }
            else {
                return new NullModuleDetector();
            }
        }
        private void returnIfCanceled() throws InterruptedException {
            if (Thread.interrupted()) {
                throw createInterruptedException();
            }
        }

        private InterruptedException createInterruptedException() {
            return new InterruptedException("Canceling parsing since build has been aborted.");
        }

    }

    @Extension
    public static class Descriptor extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Sets.newHashSet(FilePath.class, EnvVars.class, TaskListener.class, Run.class);
        }

        @Override
        public String getFunctionName() {
            return "parseWarnings";
        }

        @Override
        public String getDisplayName() {
            return "Parse warnings in files or in the console log";
        }
    }
}
