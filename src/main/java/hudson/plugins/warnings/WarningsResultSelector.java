package hudson.plugins.warnings;

import javax.annotation.CheckForNull;
import java.util.List;

import hudson.model.Run;
import hudson.plugins.analysis.core.ResultSelector;

/**
 * Selects warnings actions using the parser group.
 *
 * @author Ulli Hafner
 */
public class WarningsResultSelector implements ResultSelector {
    private final String group;

    /**
     * Creates a new instance of {@link WarningsResultSelector}.
     *
     * @param group
     *            the parser group
     */
    public WarningsResultSelector(@CheckForNull final String group) {
        this.group = group;
    }

    @Override
    public WarningsResultAction get(final Run<?, ?> build) {
        List<WarningsResultAction> actions = build.getActions(WarningsResultAction.class);
        if (group != null) {
            for (WarningsResultAction action : actions) {
                if (group.equals(action.getParser())) {
                    return action;
                }
            }
        }
        if (!actions.isEmpty() && actions.get(0).getParser() == null) { // fallback 3.x
            return actions.get(0);
        }
        return null;
    }
}

