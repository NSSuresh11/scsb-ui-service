package org.jasig.cas.client.configuration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;

public class SystemPropertiesConfigurationStrategyImpl extends BaseConfigurationStrategy {

    @Override
    public void init(final FilterConfig filterConfig, final Class<? extends Filter> filterClazz) {
    }

    @Override
    protected String get(final ConfigurationKey configurationKey) {
        return System.getProperty(configurationKey.getName());
    }
}
