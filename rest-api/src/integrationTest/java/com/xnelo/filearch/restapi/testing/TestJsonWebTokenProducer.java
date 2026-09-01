package com.xnelo.filearch.restapi.testing;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class TestJsonWebTokenProducer {

  private String userIdToUse = UUID.randomUUID().toString();

  public void setUserIdToUse(String userIdToUse) {
    this.userIdToUse = userIdToUse;
  }

  @Produces
  @Mock
  @ApplicationScoped
  public JsonWebToken mockJsonWebToken() {

    final String userIdToUseFinal =
        this.userIdToUse == null ? UUID.randomUUID().toString() : this.userIdToUse;

    return new JsonWebToken() {
      private final Map<String, Object> claims =
          Map.of(
              "sub", userIdToUseFinal,
              "given_name", "Alice",
              "family_name", "Cooper",
              "preferred_username", "alice1",
              "email", "alice@garbage.com");
      private final Set<String> claimNames = claims.keySet();

      @Override
      public String getName() {
        return getClaim("sub");
      }

      @Override
      public Set<String> getClaimNames() {
        return claimNames;
      }

      @Override
      public <T> T getClaim(String claimName) {
        Object claimValue = claims.get(claimName);
        if (claimValue == null) return null;
        return (T) claimValue;
      }
    };
  }
}
