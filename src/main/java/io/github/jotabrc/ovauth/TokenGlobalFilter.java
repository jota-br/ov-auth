package io.github.jotabrc.ovauth;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.InvalidKeyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TokenGlobalFilter extends OncePerRequestFilter {

    /**
     * Checks if requests contains valid Headers.
     * @param request Received request to be checked.
     * @param response
     * @param filterChain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String headerData =  request.getHeader(TokenCreator.HEADER_SECURE_DATA);
        String headerOrigin =  request.getHeader(TokenCreator.HEADER_SECURE_ORIGIN);

        try {
            if (headerData != null && headerOrigin != null) {
                HeaderSecurity.compare(headerData, headerOrigin);
            } else {
                throw new AccessDeniedException("Access denied");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException | AccessDeniedException |
                 java.security.InvalidKeyException e) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        String token =  request.getHeader(TokenCreator.HEADER_AUTHORIZATION);
        try {
            if(token != null && !token.isEmpty()) {
                token = token.substring(7).trim();
                TokenObject tokenObject = TokenCreator.create(token, TokenConfig.PREFIX, TokenConfig.KEY);

                List<SimpleGrantedAuthority> authorities = authorities(tokenObject.getRoles());

                UsernamePasswordAuthenticationToken userToken =
                        new UsernamePasswordAuthenticationToken(
                                tokenObject.getSubject(),
                                null,
                                authorities);

                SecurityContextHolder.getContext().setAuthentication(userToken);

            } else {
                SecurityContextHolder.clearContext();
            }
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException e) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
        }
    }

    private List<SimpleGrantedAuthority> authorities(List<String> roles){
        return roles.stream().map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
