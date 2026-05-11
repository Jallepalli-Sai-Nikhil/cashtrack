package com.cashtrack.common.security;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@GrpcGlobalServerInterceptor
public class JwtSecurityInterceptor implements ServerInterceptor {

    public static final Context.Key<String> AUTHORIZATION_CONTEXT_KEY = Context.key("token");
    
    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        String rawToken = headers.get(AUTHORIZATION_METADATA_KEY);
        
        // Put the raw token into the context so ValidateToken can use it as a fallback
        Context context = Context.current().withValue(AUTHORIZATION_CONTEXT_KEY, rawToken);
        
        // Skip authentication for Auth service public methods
        if (fullMethodName.contains("AuthService/Login") || 
            fullMethodName.contains("AuthService/Register") ||
            fullMethodName.contains("AuthService/ValidateToken") ||
            fullMethodName.contains("AuthService/RefreshToken") ||
            fullMethodName.contains("AuthService/Logout")) {
            return Contexts.interceptCall(context, call, headers, next);
        }

        if (rawToken != null && rawToken.startsWith("Bearer ")) {
            String token = rawToken.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                List<String> roles = jwtTokenProvider.getRolesFromToken(token);

                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Wrap next.startCall with the new context
                return Contexts.interceptCall(context, call, headers, next);
            } else {
                call.close(Status.UNAUTHENTICATED.withDescription("Invalid or expired JWT token"), new Metadata());
                return new ServerCall.Listener<ReqT>() {};
            }
        }

        return Contexts.interceptCall(context, call, headers, next);
    }
}
