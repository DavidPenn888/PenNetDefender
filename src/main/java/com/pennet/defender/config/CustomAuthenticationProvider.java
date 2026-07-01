package com.pennet.defender.config;

import com.pennet.defender.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private LoginAttemptService loginAttemptService;

    private final DaoAuthenticationProvider delegate;

    public CustomAuthenticationProvider(@Lazy UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.delegate = new DaoAuthenticationProvider();
        this.delegate.setUserDetailsService(userDetailsService);
        this.delegate.setPasswordEncoder(passwordEncoder);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String ip = getClientIP();
        if (loginAttemptService.isBlocked(ip)) {
            throw new LockedException("Your account has been locked due to too many failed login attempts.");
        }

        // Kaptcha validation
        String userKaptcha = request.getParameter("kaptcha");
        String sessionKaptcha = (String) request.getSession().getAttribute(com.pennet.defender.controller.KaptchaController.KAPTCHA_SESSION_KEY);

        if (sessionKaptcha == null) {
            throw new BadCredentialsException("Kaptcha is missing.");
        }

        if (userKaptcha == null || !userKaptcha.equalsIgnoreCase(sessionKaptcha)) {
            throw new BadCredentialsException("Invalid Kaptcha");
        }

        // Remove kaptcha from session after validation
        request.getSession().removeAttribute(com.pennet.defender.controller.KaptchaController.KAPTCHA_SESSION_KEY);

        try {
            return delegate.authenticate(authentication);
        } catch (BadCredentialsException e) {
            // This is handled by the AuthenticationFailureBadCredentialsEvent listener
            throw e;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return delegate.supports(authentication);
    }

    private String getClientIP() {
        final String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
