package com.multiagent.security.auth;

import spark.Request;

import java.util.List;

public class AuthFilter {
    private final JwtValidator validator;

    public AuthFilter(JwtValidator validator) { this.validator = validator; }

    public List<String> extractRoles(Request req) throws Exception {
        String auth = req.headers("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) throw new SecurityException("missing_token");
        return validator.getRoles(auth.substring(7));
    }
}
