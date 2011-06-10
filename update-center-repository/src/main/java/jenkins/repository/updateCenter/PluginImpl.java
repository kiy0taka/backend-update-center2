package jenkins.repository.updateCenter;

import hudson.Extension;
import hudson.Plugin;
import hudson.util.PluginServletFilter;

@Extension
public class PluginImpl extends Plugin {

    @Override
    public void start() throws Exception {
        PluginServletFilter.addFilter(new UpdateCenterFilter());
    }
}
