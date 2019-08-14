package sparkles.support.javalin.keycloak;

import io.javalin.core.security.AccessManager;
import sparkles.support.javalin.jwt.JwtAccessManager;
import sparkles.support.javalin.jwt.RoleProvider;

public class KeycloakAccessManager extends JwtAccessManager {

  private KeycloakAccessManager(String baseUrl, String realm, RoleProvider roleProvider) {
    super(new KeycloakPublicKeyProvider(baseUrl, realm), roleProvider);
  }

  public static AccessManager create(String baseUrl, String realm, RoleProvider roleProvider) {
    return new KeycloakAccessManager(baseUrl, realm, roleProvider);
  }

}
