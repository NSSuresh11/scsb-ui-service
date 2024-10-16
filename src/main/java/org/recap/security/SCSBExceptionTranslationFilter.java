package org.recap.security;

import org.apache.commons.lang3.StringUtils;
import org.recap.PropertyKeyConstants;
import org.recap.ScsbConstants;
import org.recap.util.HelperUtil;
import org.recap.util.PropertyUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.ThrowableAnalyzer;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by sheiks on 25/01/17.
 */
public class SCSBExceptionTranslationFilter extends GenericFilterBean {

    private CASPropertyProvider casPropertyProvider;

    // ~ Instance fields
    // ================================================================================================

    private AccessDeniedHandler accessDeniedHandler = new AccessDeniedHandlerImpl();
    private AuthenticationEntryPoint authenticationEntryPoint;
    private AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();
    private ThrowableAnalyzer throwableAnalyzer = new DefaultThrowableAnalyzer();

    private RequestCache requestCache = new HttpSessionRequestCache();

    /**
     * Instantiates a new ReCAPExceptionTranslationFilter.
     *
     * @param casPropertyProvider      the cas property provider
     * @param authenticationEntryPoint the authentication entry point
     */
    public SCSBExceptionTranslationFilter(CASPropertyProvider casPropertyProvider, AuthenticationEntryPoint authenticationEntryPoint) {
        this(authenticationEntryPoint, new HttpSessionRequestCache());
        this.casPropertyProvider = casPropertyProvider;
    }

    /**
     * Instantiates a ReCAPExceptionTranslationFilter.
     *
     * @param authenticationEntryPoint the authentication entry point
     * @param requestCache             the request cache
     */
    public SCSBExceptionTranslationFilter(AuthenticationEntryPoint authenticationEntryPoint,
                                          RequestCache requestCache) {
        Assert.notNull(authenticationEntryPoint,
                "authenticationEntryPoint cannot be null");
        Assert.notNull(requestCache, "requestCache cannot be null");
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.requestCache = requestCache;
    }

    // ~ Methods
    // ========================================================================================================

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(authenticationEntryPoint,"authenticationEntryPoint must be specified");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        try {
            chain.doFilter(request, response);

            logger.debug("Chain processed normally");
        }
        catch (IOException ex) {
            throw ex;
        }
        catch (Exception ex) {
            // Try to extract a SpringSecurityException from the stacktrace
            Throwable[] causeChain = throwableAnalyzer.determineCauseChain(ex);
            RuntimeException ase = (AuthenticationException) throwableAnalyzer
                    .getFirstThrowableOfType(AuthenticationException.class, causeChain);

            if (ase == null) {
                ase = (AccessDeniedException) throwableAnalyzer.getFirstThrowableOfType(
                        AccessDeniedException.class, causeChain);
            }

            if (ase != null) {
                handleSpringSecurityException(request, response, chain, ase);
            }
            else {
                // Rethrow ServletExceptions and RuntimeExceptions as-is
                if (ex instanceof ServletException) {
                    throw (ServletException) ex;
                }
                else if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                }

                // Wrap other Exceptions. This shouldn't actually happen
                // as we've already covered all the possibilities for doFilter
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Gets authentication entry point.
     *
     * @return the authentication entry point
     */
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    /**
     * Gets authentication trust resolver.
     *
     * @return the authentication trust resolver
     */
    protected AuthenticationTrustResolver getAuthenticationTrustResolver() {
        return authenticationTrustResolver;
    }

    private void handleSpringSecurityException(HttpServletRequest request,
                                               HttpServletResponse response, FilterChain chain, RuntimeException exception)
            throws IOException, ServletException {
        if (exception instanceof AuthenticationException) {
            logger.debug(
                    "Authentication exception occurred; redirecting to authentication entry point",
                    exception);

            sendStartAuthentication(request, response, chain,
                    (AuthenticationException) exception);
        }
        else if (exception instanceof AccessDeniedException) {
            if (authenticationTrustResolver.isAnonymous(SecurityContextHolder
                    .getContext().getAuthentication())) {
                logger.debug(
                        "Access is denied (user is anonymous); redirecting to authentication entry point",
                        exception);

                sendStartAuthentication(
                        request,
                        response,
                        chain,
                        new InsufficientAuthenticationException(
                                "Full authentication is required to access this resource"));
            }
            else {
                logger.debug(
                        "Access is denied (user is not anonymous); delegating to AccessDeniedHandler",
                        exception);

                accessDeniedHandler.handle(request, response,
                        (AccessDeniedException) exception);
            }
        }
    }

    /**
     * Send start authentication. Default is OAuth (NYPL). CAS authentication will be decided based on the institution for PUL and CUL
     *
     * @param request  the request
     * @param response the response
     * @param chain    the chain
     * @param reason   the reason
     * @throws ServletException the servlet exception
     * @throws IOException      the io exception
     */
    protected void sendStartAuthentication(HttpServletRequest request,
                                           HttpServletResponse response, FilterChain chain,
                                           AuthenticationException reason) throws ServletException, IOException {
        // SEC-112: Clear the SecurityContextHolder's Authentication, as the
        // existing Authentication is no longer considered valid
        SecurityContextHolder.getContext().setAuthentication(null);
        requestCache.saveRequest(request, response);
        logger.info("Calling Authentication entry point.");
        String institution = HelperUtil.getInstitutionFromRequest(request);
        if (StringUtils.isNotBlank(institution)) {
            String authType = HelperUtil.getBean(PropertyUtil.class).getPropertyByInstitutionAndKey(institution, PropertyKeyConstants.ILS.ILS_AUTH_TYPE);
            if(StringUtils.equals(authType, ScsbConstants.AUTH_TYPE_OAUTH)) {
                this.authenticationEntryPoint.commence(request,response,reason);
            } else {
                String url = HelperUtil.getBean(PropertyUtil.class).getPropertyByInstitutionAndKey(institution, PropertyKeyConstants.ILS.ILS_AUTH_SERVICE_LOGIN);

                //Calling cas entry point based on institution type.
                CasAuthenticationEntryPoint casAuthenticationEntryPoint = new CasAuthenticationEntryPoint();
                casAuthenticationEntryPoint.setLoginUrl(url);
                casAuthenticationEntryPoint.setServiceProperties(casPropertyProvider.getServiceProperties());
                casAuthenticationEntryPoint.commence(request, response, reason);
            }
        } else {
            response.sendRedirect("/");
        }

    }

    /**
     * Sets access denied handler.
     *
     * @param accessDeniedHandler the access denied handler
     */
    public void setAccessDeniedHandler(AccessDeniedHandler accessDeniedHandler) {
        Assert.notNull(accessDeniedHandler, "AccessDeniedHandler required");
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * Sets authentication trust resolver.
     *
     * @param authenticationTrustResolver the authentication trust resolver
     */
    public void setAuthenticationTrustResolver(
            AuthenticationTrustResolver authenticationTrustResolver) {
        Assert.notNull(authenticationTrustResolver,
                "authenticationTrustResolver must not be null");
        this.authenticationTrustResolver = authenticationTrustResolver;
    }

    /**
     * Sets throwable analyzer.
     *
     * @param throwableAnalyzer the throwable analyzer
     */
    public void setThrowableAnalyzer(ThrowableAnalyzer throwableAnalyzer) {
        Assert.notNull(throwableAnalyzer, "throwableAnalyzer must not be null");
        this.throwableAnalyzer = throwableAnalyzer;
    }

    /**
     * Default implementation of <code>ThrowableAnalyzer</code> which is capable of also
     * unwrapping <code>ServletException</code>s.
     */
    private static final class DefaultThrowableAnalyzer extends ThrowableAnalyzer {
        /**
         * @see ThrowableAnalyzer#initExtractorMap()
         */
        @Override
        protected void initExtractorMap() {
            super.initExtractorMap();

            registerExtractor(ServletException.class, throwable -> {
                ThrowableAnalyzer.verifyThrowableHierarchy(throwable,
                        ServletException.class);
                return ((ServletException) throwable).getRootCause();
            });
        }

    }


}
