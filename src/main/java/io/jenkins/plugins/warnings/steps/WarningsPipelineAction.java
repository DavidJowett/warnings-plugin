package io.jenkins.plugins.warnings.steps;

import org.kohsuke.stapler.StaplerRequest;

import hudson.model.Job;
import hudson.plugins.analysis.core.AbstractProjectAction;
import hudson.plugins.analysis.core.BuildResult;
import hudson.plugins.analysis.core.ResultAction;
import hudson.plugins.analysis.graph.DefaultGraphConfigurationView;
import hudson.plugins.analysis.graph.GraphConfigurationView;
import hudson.plugins.analysis.graph.UserGraphConfigurationView;
import hudson.plugins.warnings.Messages;
import hudson.plugins.warnings.WarningsDescriptor;
import hudson.plugins.warnings.WarningsResultAction;

/**
 * Entry point to visualize the warnings trend graph in the job screen.
 * Drawing of the graph is delegated to the associated
 * {@link WarningsResultAction}.
 *
 * @author Ulli Hafner
 */
public class WarningsPipelineAction extends AbstractProjectAction<ResultAction<? extends BuildResult>> {
    private final String id;

    /**
     * Creates a new instance of {@link WarningsPipelineAction}.
     *
     * @param job
     *            the job that owns this action
     * @param id
     *            the group of the parsers that share this action
     */
    public WarningsPipelineAction(final Job<?, ?> job, final String id) {
        super(job, WarningsResultAction.class,
                Messages._Warnings_ProjectAction_Name(), Messages._Warnings_Trend_Name(),
                "basewarnings", WarningsDescriptor.SMALL_ICON_URL, WarningsDescriptor.getResultUrlFromId(id));
        this.id = id;
    }

    @Override
    public boolean isTrendVisible(final StaplerRequest request) {
        GraphConfigurationView configuration = createUserConfiguration(request);

        boolean canShow = configuration.isVisible() && configuration.hasMeaningfulGraph();

        return !createUserConfiguration(request, WarningsDescriptor.PLUGIN_ID).isDeactivated() && canShow;
    }

    @Override
    protected GraphConfigurationView createUserConfiguration(final StaplerRequest request) {
        return createUserConfiguration(request, id);
    }

    private UserGraphConfigurationView createUserConfiguration(final StaplerRequest request, final String urlName) {
        return new UserGraphConfigurationView(
                createConfiguration(getAvailableGraphs()), getOwner(),
                urlName, null,
                request.getCookies(), createBuildHistory());
    }

    @Override
    protected GraphConfigurationView createDefaultConfiguration() {
        return new DefaultGraphConfigurationView(
                createConfiguration(getAvailableGraphs()), getOwner(),
                id,
                createBuildHistory(), null);
    }
}

