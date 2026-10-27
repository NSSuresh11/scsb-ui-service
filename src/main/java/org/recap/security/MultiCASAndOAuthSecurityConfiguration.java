package org.recap.security;

import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.filter.CsrfCookieGeneratorFilter;
import org.recap.filter.SCSBInstitutionFilter;
import org.recap.filter.SCSBLogoutFilter;
import org.recap.filter.SCSBValidationFilter;
import org.recap.service.CustomUserDetailsService;
import org.recap.util.UserAuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfFilter;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

/**
 * Created by sheiks on 30/01/17.
 */
@Configuration
@EnableMethodSecurity
public class MultiCASAndOAuthSecurityConfiguration {

    @Value("${" + PropertyKeyConstants.CAS_DEFAULT_URL_PREFIX + "}")
    private String casUrlPrefix;

    @Value("${" + PropertyKeyConstants.CAS_DEFAULT_SERVICE_LOGOUT + "}")
    private String casServiceLogout;

    @Value("${" + PropertyKeyConstants.SCSB_APP_SERVICE_LOGOUT + "}")
    private String appServiceLogout;

    @Value("${" + PropertyKeyConstants.SCSB_UI_URL + "}")
    private String scsbUiUrl;

    @Value("${" + ScsbConstants.CSP_ENABLE + "}")
    private Boolean cspEnable;

    @Value("${" + ScsbConstants.CSP_VALUE + "}")
    private String cspValue;

    @Value("${" + ScsbConstants.FRAME_ANCESTOR_VALUE + "}")
    private String frameAncestorValue;

    @Autowired
    private CASPropertyProvider casPropertyProvider;

    @Autowired
    private UserAuthUtil userAuthUtil;

    @Autowired
    private ApplicationContext applicationContext;



    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        String loginPath = "/login";
        LoginUrlAuthenticationEntryPoint loginUrlAuthenticationEntryPoint = new LoginUrlAuthenticationEntryPoint(loginPath);
        SCSBExceptionTranslationFilter SCSBExceptionTranslationFilter = new SCSBExceptionTranslationFilter(casPropertyProvider, loginUrlAuthenticationEntryPoint);
        http.addFilterAfter(new CsrfCookieGeneratorFilter(), CsrfFilter.class)
                .addFilterAfter(new SCSBInstitutionFilter(), CsrfCookieGeneratorFilter.class)
                .addFilterAfter(SCSBExceptionTranslationFilter, ExceptionTranslationFilter.class)
                .exceptionHandling(ex -> ex.authenticationEntryPoint(loginUrlAuthenticationEntryPoint))
                .addFilter(casAuthenticationFilter())
                .addFilterBefore(reCAPLogoutFilter(), LogoutFilter.class)
                .addFilterBefore(requestCasGlobalLogoutFilter(), LogoutFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/actuator", "/actuator/prometheus", "/login").permitAll()
                        .requestMatchers("/auth/saml", "/saml/metadata/**", "/saml/acs", "/saml/acs/**").permitAll()
                        .requestMatchers("/**").authenticated().anyRequest().authenticated()
                )
                .sessionManagement(sm -> sm.invalidSessionUrl("/home"))

                .headers(headers -> {
                    if (Boolean.TRUE.equals(cspEnable)) {
                        headers.contentSecurityPolicy(csp ->
                                csp.policyDirectives("default-src " + scsbUiUrl + " " + cspValue   + "; frame-ancestors " + " " + frameAncestorValue));
                    }
                })

                .logout(logout -> logout
                        .logoutUrl(ScsbConstants.LOG_USER_LOGOUT_URL)
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    /**
     * Register the CAS global logout filter.
     *
     * @return the LogoutFilter
     */
    @Bean
    public LogoutFilter requestCasGlobalLogoutFilter() {
        String logoutSuccessUrl = casServiceLogout + "?service=" + appServiceLogout;
        SCSBSimpleUrlLogoutSuccessHandler SCSBSimpleUrlLogoutSuccessHandler = new SCSBSimpleUrlLogoutSuccessHandler(userAuthUtil);
        SCSBSimpleUrlLogoutSuccessHandler.setDefaultTargetUrl(logoutSuccessUrl);
        LogoutFilter logoutFilter = new LogoutFilter(SCSBSimpleUrlLogoutSuccessHandler, new SecurityContextLogoutHandler());
      //  logoutFilter.setLogoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"));
        return logoutFilter;
    }

    /**
     * Register the ReCAP logout filter.
     *
     * @return the ReCAPLogoutFilter
     */
    @Bean
    public SCSBLogoutFilter reCAPLogoutFilter() {
        return new SCSBLogoutFilter();
    }

    /**
     * Cas authentication filter cas authentication filter.
     *
     * @return the cas authentication filter
     * @throws Exception the exception
     */
    @Bean
    public CasAuthenticationFilter casAuthenticationFilter() throws Exception {
        CasAuthenticationFilter casAuthenticationFilter = new CasAuthenticationFilter();
        casAuthenticationFilter.setFilterProcessesUrl("/j_spring_cas_security_check");
        casAuthenticationFilter.setAuthenticationManager(authenticationManager());
        return casAuthenticationFilter;
    }

    @Bean
    FilterRegistrationBean<SCSBValidationFilter> filterRegistrationBean(){
        FilterRegistrationBean<SCSBValidationFilter>  filterRegistrationBean = new FilterRegistrationBean();
        SCSBValidationFilter scsbValidationFilter = new SCSBValidationFilter();
        filterRegistrationBean.setFilter(scsbValidationFilter);
        filterRegistrationBean.addUrlPatterns("/collection/*","/search/*","/request/*","/reports/*","/userRoles/*","/bulkRequest/*","/roles/*","/jobs/*","/openMarcRecordByBibId/*","/admin/*","/dataExport/*","/request-log/*");
        return filterRegistrationBean;
    }

    @Bean
    protected AuthenticationManager authenticationManager() throws Exception {
        return new ProviderManager(Arrays.asList(casAuthenticationProvider()));
    }


    /**
     * Register CAS authentication provider and set properties
     *
     * @return the CasAuthenticationProvider
     */
    @Bean
    public CasAuthenticationProvider casAuthenticationProvider() {
        CasAuthenticationProvider casAuthenticationProvider = new CasAuthenticationProvider();
        casAuthenticationProvider.setAuthenticationUserDetailsService(authenticationUserDetailsService());
        casAuthenticationProvider.setServiceProperties(casPropertyProvider.getServiceProperties());
        casAuthenticationProvider.setTicketValidator(cas20ServiceTicketValidator());
        casAuthenticationProvider.setKey("an_id_for_this_auth_provider_only");
        return casAuthenticationProvider;
    }

    /**
     * Register Authentication user details service .
     *
     * @return the AuthenticationUserDetailsService
     */
    @Bean
    public AuthenticationUserDetailsService authenticationUserDetailsService() {
        return new CustomUserDetailsService();
    }


    /**
     * Register ReCAPCas20ServiceTicketValidator.
     *
     * @return the ReCAPCas20ServiceTicketValidator
     */
    @Bean
    public SCSBCas20ServiceTicketValidator cas20ServiceTicketValidator() {
        return new SCSBCas20ServiceTicketValidator(casUrlPrefix);
    }


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() throws Exception {
        return (web)->web.ignoring().requestMatchers("/resources/**", "/static/**", "/assets/**", "/index.html", "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.gif", "/**/*.svg", "/**/favicon.ico","/**/*.bmp","/**/*.jpeg","/**/*.ttf","/**/*.eot","/**/*.svg","/**/*.woff","/**/*.woff2","/images/**").
                requestMatchers("/collection/**","/search/**","/request/**","/reports/**","/userRoles/**","/bulkRequest/**","/roles/**","/jobs/**","/openMarcRecordByBibId/**","/admin/**","/api/**","/dataExport/**","/validation/**","/actuator/**","/monitoring/**","/request-log/**");
    }

    /**
     * Register Http session event publisher for SCSB.
     *
     * @return the ReCAPHttpSessionEventPublisher
     */
    @Bean
    public SCSBHttpSessionEventPublisher httpSessionEventPublisher() {
        return new SCSBHttpSessionEventPublisher();
    }


  /*  @Bean
    WebClient webClient(ClientRegistrationRepository clientRegistrationRepository,
                        OAuth2AuthorizedClientRepository authorizedClientRepository) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrationRepository,
                        authorizedClientRepository);
        oauth2.setDefaultOAuth2AuthorizedClient(true);
        return WebClient.builder()
                .apply(oauth2.oauth2Configuration())
                .build();
    }
*/
}