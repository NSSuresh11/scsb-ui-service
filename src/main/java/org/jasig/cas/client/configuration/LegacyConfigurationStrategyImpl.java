package org.jasig.cas.client.configuration;

import org.jasig.cas.client.util.CommonUtils;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;

public final class LegacyConfigurationStrategyImpl extends BaseConfigurationStrategy {

    private final WebXmlConfigurationStrategyImpl webXmlConfigurationStrategy = new WebXmlConfigurationStrategyImpl();

    private final JndiConfigurationStrategyImpl jndiConfigurationStrategy = new JndiConfigurationStrategyImpl();

    @Override
    public void init(final FilterConfig filterConfig, final Class<? extends Filter> filterClazz) {
        this.webXmlConfigurationStrategy.init(filterConfig, filterClazz);
        this.jndiConfigurationStrategy.init(filterConfig, filterClazz);
    }

    @Override
    protected String get(final ConfigurationKey key) {
        final String value1 = this.webXmlConfigurationStrategy.get(key);

        if (CommonUtils.isNotBlank(value1)) {
            return value1;
        }

        return this.jndiConfigurationStrategy.get(key);
    }
}
