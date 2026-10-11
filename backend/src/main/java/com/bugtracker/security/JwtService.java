package com.bugtracker.security;
import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.security.SecureRandom; import java.util.*;
@Service public class JwtService {
 private final SecretKey key; private final long expiration;
 public JwtService(@Value("${app.jwt.secret:}") String secret,@Value("${app.jwt.expiration-ms}") long expiration,@Value("${app.environment:development}") String environment){
  byte[] keyBytes;
  if(secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32) keyBytes=secret.getBytes(StandardCharsets.UTF_8);
  else if("production".equalsIgnoreCase(environment)) throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes in production");
  else { keyBytes=new byte[32]; new SecureRandom().nextBytes(keyBytes); System.err.println("WARNING: JWT_SECRET is unset or too short; using a temporary development key. Tokens will expire when the application restarts."); }
  key=Keys.hmacShaKeyFor(keyBytes); this.expiration=expiration;
 }
 public String generate(String email,String role,Long id){ return Jwts.builder().subject(email).claim("role",role).claim("id",id).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+expiration)).signWith(key).compact(); }
 public Claims parse(String token){ return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
