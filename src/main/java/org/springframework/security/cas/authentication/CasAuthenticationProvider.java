package org.springframework.security.cas.authentication;

import lombok.extern.slf4j.Slf4j;
import org.apereo.cas.client.proxy.Cas20ProxyRetriever;
import org.apereo.cas.client.validation.Assertion;
import org.apereo.cas.client.validation.TicketValidationException;
import org.apereo.cas.client.validation.TicketValidator;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.security.SCSBCas20ServiceTicketValidator;
import org.recap.util.HelperUtil;
import org.recap.util.PropertyUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.NullAuthoritiesMapper;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Created by sheiks on 20/01/17.
 */
@Slf4j
public class CasAuthenticationProvider implements AuthenticationProvider,
        InitializingBean, MessageSourceAware {
    // ~ Static fields/initializers
    // =====================================================================================



    // ~ Instance fields
    // ================================================================================================
    private final UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();
    /**
     * The Messages.
     */
    protected MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();
    private AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService;
    private StatelessTicketCache statelessTicketCache = new NullStatelessTicketCache();
    private String key;
    private TicketValidator ticketValidator;
    private ServiceProperties serviceProperties;
    private GrantedAuthoritiesMapper authoritiesMapper = new NullAuthoritiesMapper();

    // ~ Methods
    // ========================================================================================================

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.authenticationUserDetailsService,
                "An authenticationUserDetailsService must be set");
        Assert.notNull(this.ticketValidator, "A ticketValidator must be set");
        Assert.notNull(this.statelessTicketCache, "A statelessTicketCache must be set");
        Assert.hasText(
                this.key,
                "A Key is required so CasAuthenticationProvider can identify tokens it previously authenticated");
        Assert.notNull(this.messages, "A message source must be set");
    }

    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        log.info("Calling authenticate >>>> ");
        if (!supports(authentication.getClass())) {
            return null;
        }

        if (authentication instanceof UsernamePasswordAuthenticationToken
                && (!CasServiceTicketAuthenticationToken.CAS_STATEFUL_IDENTIFIER
                .equals(authentication.getPrincipal().toString()) && !CasServiceTicketAuthenticationToken.CAS_STATELESS_IDENTIFIER
                .equals(authentication.getPrincipal().toString()))) {
            // UsernamePasswordAuthenticationToken not CAS related
            log.info("Calling UsernamePasswordAuthenticationToken >>>> ");

            return null;
        }

        // If an existing CasAuthenticationToken, just check we created it
        if (authentication instanceof CasAuthenticationToken) {
            if (this.key.hashCode() == ((CasAuthenticationToken) authentication)
                    .getKeyHash()) {
                log.info("Calling CasAuthenticationToken >>>> ");

                return authentication;
            } else {
                log.info("Calling BadCredentialsException >>>> ");

                throw new BadCredentialsException(
                        messages.getMessage("CasAuthenticationProvider.incorrectKey",
                                "The presented CasAuthenticationToken does not contain the expected key"));
            }
        }

        // Ensure credentials are presented
        if ((authentication.getCredentials() == null)
                || "".equals(authentication.getCredentials())) {
            log.info("Calling BadCredentialsException null >>>> ");

            throw new BadCredentialsException(messages.getMessage(
                    "CasAuthenticationProvider.noServiceTicket",
                    "Failed to provide a CAS service ticket to validate"));
        }

        boolean stateless = false;

        if (authentication instanceof UsernamePasswordAuthenticationToken
                && CasServiceTicketAuthenticationToken.CAS_STATELESS_IDENTIFIER.equals(authentication
                .getPrincipal())) {
            stateless = true;
        }

        CasAuthenticationToken result = null;

        if (stateless) {
            // Try to obtain from cache
            result = statelessTicketCache.getByTicketId(authentication.getCredentials()
                    .toString());
        }

        if (result == null) {
            log.info("Calling authenticateNow >>>>>>");
            result = this.authenticateNow(authentication);
            result.setDetails(authentication.getDetails());
        }

        if (stateless) {
            // Add to cache
            statelessTicketCache.putTicketInCache(result);
        }

        return result;
    }

    /**
     * Authenticate the CAS users. CAS URL will be decided based on the user institution.
     *
     * @return CasAuthenticationToken
     */
    private CasAuthenticationToken authenticateNow(final Authentication authentication)
            throws AuthenticationException {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            log.info("Inside authenticateNow >>>>>");
            String institution = (String) ((ServletRequestAttributes) requestAttributes).getRequest().getAttribute(ScsbConstants.SCSB_INSTITUTION_CODE);

            String casServerUrl = HelperUtil.getBean(PropertyUtil.class).getPropertyByInstitutionAndKey(institution, PropertyKeyConstants.ILS.ILS_AUTH_SERVICE_PREFIX);

            SCSBCas20ServiceTicketValidator ticketValidator = (SCSBCas20ServiceTicketValidator) this.ticketValidator;
            ticketValidator.setCasServerUrlPrefix(casServerUrl);
            ticketValidator.setProxyRetriever(new Cas20ProxyRetriever(casServerUrl, "UTF-8", ticketValidator.getURLConnectionFactory()));

            final Assertion assertion = this.ticketValidator.validate(authentication
                    .getCredentials().toString(), getServiceUrl(authentication));
            final UserDetails userDetails = loadUserByAssertion(assertion);
            userDetailsChecker.check(userDetails);
            log.info("userDetails >>>>>>> " + userDetails);
            return new CasAuthenticationToken(this.key, userDetails,
                    authentication.getCredentials(),
                    authoritiesMapper.mapAuthorities(userDetails.getAuthorities()),
                    userDetails, assertion);
        } catch (final TicketValidationException e) {
            throw new BadCredentialsException(e.getMessage(), e);
        }
    }

    /**
     * Gets the serviceUrl. If the {@link Authentication#getDetails()} is an instance of
     * {@link ServiceAuthenticationDetails}, then
     * {@link ServiceAuthenticationDetails#getServiceUrl()} is used. Otherwise, the
     * {@link ServiceProperties#getService()} is used.
     */
    private String getServiceUrl(Authentication authentication) {
        String serviceUrl;
        if (authentication.getDetails() instanceof ServiceAuthenticationDetails) {
            serviceUrl = ((ServiceAuthenticationDetails) authentication.getDetails())
                    .getServiceUrl();
        } else if (serviceProperties == null) {
            throw new IllegalStateException(
                    "serviceProperties cannot be null unless Authentication.getDetails() implements ServiceAuthenticationDetails.");
        } else if (serviceProperties.getService() == null) {
            throw new IllegalStateException(
                    "serviceProperties.getService() cannot be null unless Authentication.getDetails() implements ServiceAuthenticationDetails.");
        } else {
            serviceUrl = serviceProperties.getService();
        }
        if (log.isDebugEnabled()) {
            log.debug("serviceUrl = " + serviceUrl);
        }
        return serviceUrl;
    }

    /**
     * Template method for retrieving the UserDetails based on the assertion. Default is
     * to call configured userDetailsService and pass the username. Deployers can override
     * this method and retrieve the user based on any criteria they desire.
     *
     * @param assertion The CAS Assertion.
     * @return the UserDetails.
     */
    protected UserDetails loadUserByAssertion(final Assertion assertion) {
        final CasAssertionAuthenticationToken token = new CasAssertionAuthenticationToken(
                assertion, "");
        return this.authenticationUserDetailsService.loadUserDetails(token);
    }

    /**
     * Sets user details service.
     *
     * @param userDetailsService the user details service
     */
    @SuppressWarnings("unchecked")
    /**
     * Sets the UserDetailsService to use. This is a convenience method to invoke
     */
    public void setUserDetailsService(final UserDetailsService userDetailsService) {
        this.authenticationUserDetailsService = new UserDetailsByNameServiceWrapper(
                userDetailsService);
    }

    /**
     * Sets authentication user details service.
     *
     * @param authenticationUserDetailsService the authentication user details service
     */
    public void setAuthenticationUserDetailsService(
            final AuthenticationUserDetailsService<CasAssertionAuthenticationToken> authenticationUserDetailsService) {
        this.authenticationUserDetailsService = authenticationUserDetailsService;
    }

    /**
     * Sets service properties.
     *
     * @param serviceProperties the service properties
     */
    public void setServiceProperties(final ServiceProperties serviceProperties) {
        this.serviceProperties = serviceProperties;
    }

    /**
     * Gets key.
     *
     * @return the key
     */
    protected String getKey() {
        return key;
    }

    /**
     * Sets key.
     *
     * @param key the key
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets stateless ticket cache.
     *
     * @return the stateless ticket cache
     */
    public StatelessTicketCache getStatelessTicketCache() {
        return statelessTicketCache;
    }

    /**
     * Sets stateless ticket cache.
     *
     * @param statelessTicketCache the stateless ticket cache
     */
    public void setStatelessTicketCache(final StatelessTicketCache statelessTicketCache) {
        this.statelessTicketCache = statelessTicketCache;
    }

    /**
     * Gets ticket validator.
     *
     * @return the ticket validator
     */
    protected TicketValidator getTicketValidator() {
        return ticketValidator;
    }

    /**
     * Sets ticket validator.
     *
     * @param ticketValidator the ticket validator
     */
    public void setTicketValidator(final TicketValidator ticketValidator) {
        this.ticketValidator = ticketValidator;
    }

    public void setMessageSource(final MessageSource messageSource) {
        this.messages = new MessageSourceAccessor(messageSource);
    }

    /**
     * Sets authorities mapper.
     *
     * @param authoritiesMapper the authorities mapper
     */
    public void setAuthoritiesMapper(GrantedAuthoritiesMapper authoritiesMapper) {
        this.authoritiesMapper = authoritiesMapper;
    }

    public boolean supports(final Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication))
                || (CasAuthenticationToken.class.isAssignableFrom(authentication))
                || (CasAssertionAuthenticationToken.class
                .isAssignableFrom(authentication));
    }
}

