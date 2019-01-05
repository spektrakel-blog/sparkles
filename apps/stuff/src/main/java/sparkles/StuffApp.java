package sparkles;

import org.hsqldb.jdbc.JDBCDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.Persistence;
import javax.sql.DataSource;

import io.javalin.Javalin;

import sparkles.support.common.Environment;
import sparkles.support.javalin.JavalinApp;
import sparkles.support.javalin.flyway.FlywayExtension;
import sparkles.support.javalin.keycloak.security.KeycloakAccessManager;
import sparkles.support.javalin.keycloak.security.KeycloakRoles;
import sparkles.support.javalin.spring.data.auditing.Auditing;
import sparkles.support.javalin.spring.data.auditing.AuditingExtension;
import sparkles.support.javalin.spring.data.SpringDataExtension;

import static io.javalin.apibuilder.ApiBuilder.crud;
import static sparkles.support.javalin.security.Security.requires;
import static sparkles.support.javalin.spring.data.SpringDataExtension.springData;
import static sparkles.support.javalin.spring.data.crud.CrudRepositoryHandler.crudHandler;

public class StuffApp {
  static {
    System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX + "sparkles", Environment.logLevel());
  }

  private static final Logger LOG = LoggerFactory.getLogger(StuffApp.class);

  public static void main(String[] args) {
    new StuffApp().init().start();
  }

  public Javalin init() {
    final DataSource dataSource = createDataSource();

    return JavalinApp.create()
      .register(FlywayExtension.create(() -> dataSource,
        "persistence/migrations/flyway"))
      .register(SpringDataExtension.create(() -> Persistence.createEntityManagerFactory(
        "stuff", createHibernateProperties(dataSource))))
      .register(AuditingExtension.create((ctx) -> {
        // TODO: resolve auditor from request context
        return "foo";
      }))
      .accessManager(KeycloakAccessManager.create(
        Environment.value("KEYCLOAK_URL",  "https://foobar"),
        Environment.value("KEYCLOAK_REALM", "realm"),
        (claims) -> {
          // TODO: resolve role from keycloak stuff
          return KeycloakRoles.ANYONE;
        }
      ))
      .get("/", (ctx) -> {
        StuffRepository repository = springData(ctx).createRepository(StuffRepository.class);

        List<StuffEntity> stuffs = repository.findAll();
        for (StuffEntity stuff : stuffs) {
          LOG.info("stuff is {} // {}", stuff.createdAt, stuff.createdBy);
        }

        ctx.result("count: " + stuffs.size());
      }, requires(KeycloakRoles.ANYONE))
      .post("/", (ctx) -> {
        Object auditor = Auditing.getStrategy().resolveCurrentContext().getCurrentAuditor().get();
        LOG.info("Current auditor is {}", auditor);

        StuffRepository repository = springData(ctx).createRepository(StuffRepository.class);
        StuffEntity entity = repository.save(new StuffEntity().withName("foobararar"));

        ctx.result(entity.id.toString()).status(201);
      })
      .routes(() -> {
        crud("stuff/:id", crudHandler(StuffRepository.class, StuffEntity.class, UUID::fromString));
      });
  }

  private static DataSource createDataSource() {
    final JDBCDataSource ds = new JDBCDataSource();
    ds.setUrl(Environment.value("JDBC_URL", "jdbc:hsqldb:mem:standalone"));
    ds.setUser(Environment.value("JDBC_USER", "sa"));
    ds.setPassword(Environment.value("JDBC_PASSWORD", ""));

    return ds;
  }

  private static Map<String, Object> createHibernateProperties(DataSource dataSource) {
    final Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", dataSource);
    props.put("javax.persistence.transactionType", "RESOURCE_LOCAL");
    props.put("hibernate.show_sql", Environment.isDevelop());
    props.put("hibernate.format_sql", false);
    props.put("hibernate.hbm2ddl.auto", "validate");
    props.put("hibernate.dialect", Environment.value("HIBERNATE_DIALECT", "org.hibernate.dialect.HSQLDialect"));

    return props;
  }

}
