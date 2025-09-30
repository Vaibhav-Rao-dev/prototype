package com.multiagent.security.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;

import java.net.URL;
import java.text.ParseException;
import java.util.List;

public class JwtValidator {
    private final ConfigurableJWTProcessor processor;

    public JwtValidator(String jwksUrl) throws Exception {
        this.processor = new DefaultJWTProcessor();
        JWKSource keySource = new RemoteJWKSet(new URL(jwksUrl), new DefaultResourceRetriever(2000,2000));
        JWSKeySelector keySelector = new JWSVerificationKeySelector(JWSAlgorithm.RS256, keySource);
        processor.setJWSKeySelector(keySelector);
    }

    public List<String> getRoles(String token) throws Exception {
        SignedJWT jwt = SignedJWT.parse(token);
        var claims = jwt.getJWTClaimsSet();
        Object realmAccess = claims.getClaim("realm_access");
        if (realmAccess instanceof java.util.Map) {
            Object roles = ((java.util.Map)realmAccess).get("roles");
            if (roles instanceof List) return (List<String>)roles;
        }
        // fallback: roles claim
        Object r = claims.getClaim("roles");
        if (r instanceof List) return (List<String>)r;
        return List.of();
    }
}
