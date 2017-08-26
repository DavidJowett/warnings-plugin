package io.jenkins.plugins.warnings.steps;

import java.util.Collection;

import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.analysis.core.AbstractHealthDescriptor;
import hudson.plugins.analysis.core.AbstractResultAction;
import hudson.plugins.analysis.core.PluginDescriptor;
import hudson.plugins.warnings.Messages;
import hudson.plugins.warnings.WarningsDescriptor;

/**
 * Controls the live cycle of the Checkstyle results. This action persists the
 * results of the Checkstyle analysis of a build and displays the results on the
 * build page. The actual visualization of the results is defined in the
 * matching <code>summary.jelly</code> file.
 * <p>
 * Moreover, this class renders the Checkstyle result trend.
 * </p>
 *
 * @author Ulli Hafner
 */
public class WarningsPipelineResultAction extends AbstractResultAction<AnalysisResult> {
    private final String url;

    /**
     * Creates a new instance of <code>CheckStyleResultAction</code>.
     *
     * @param owner
     *            the associated run of this action
     * @param healthDescriptor
     *            health descriptor
     * @param result
     *            the result in this build
     */
    public WarningsPipelineResultAction(final Run<?, ?> owner, final AbstractHealthDescriptor healthDescriptor,
            final AnalysisResult result, final String url) {
        super(owner, healthDescriptor, result);

        this.url = url;
    }

    @Override
    public String getUrlName() {
        return url;
    }

    @Override
    public String getDisplayName() {
        return Messages.Warnings_ProjectAction_Name();
    }

    @Override
    protected PluginDescriptor getDescriptor() {
        return new WarningsDescriptor();
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return asSet(new WarningsPipelineAction(getJob(), url));
    }
}
