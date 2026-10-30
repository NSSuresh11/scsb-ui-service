package org.recap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class StaticResourcesConfiguration implements WebMvcConfigurer {

    private static final String[] CLASSPATH_RESOURCE_LOCATIONS = {"classpath:/META-INF/resources/",
            "classpath:/resources/", "classpath:/static/", "classpath:/public/"};

    static final String[] STATIC_RESOURCES = new String[]{
            "/**/*.css",
            "/**/*.html",
            "/**/*.js",
            "/**/*.json",
            "/**/*.bmp",
            "/**/*.jpeg",
            "/**/*.jpg",
            "/**/*.png",
            "/**/*.ttf",
            "/**/*.eot",
            "/**/*.svg",
            "/**/*.woff",
            "/**/*.woff2"
    };

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //Add all static files
        Integer cachePeriod = 30;
        registry.addResourceHandler(STATIC_RESOURCES)
                .addResourceLocations(CLASSPATH_RESOURCE_LOCATIONS)
                .setCachePeriod(cachePeriod);

        //Create mapping to index.html for Angular HTML5 mode.
        String[] indexLocations = getIndexLocations();
        registry.addResourceHandler("/**")
                .addResourceLocations(indexLocations)
                .setCachePeriod(cachePeriod)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        return location.exists() && location.isReadable() ? location : null;
                    }
                });
    }

    private String[] getIndexLocations() {
        return Arrays.stream(CLASSPATH_RESOURCE_LOCATIONS)
                .map((location) -> location + "index.html")
                .toArray(String[]::new);
    }
}