package sparkles.replica;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.sql.DataSource;
import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import okhttp3.Response;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import sparkles.replica.collection.CollectionApi;
import sparkles.replica.document.DocumentApi;
import sparkles.replica.version.VersionApi;
import sparkles.support.common.Environment;
import sparkles.support.common.collections.Collections;
import sparkles.support.json.JavaxJson;
import sparkles.support.javalin.flyway.FlywayPlugin;
import sparkles.support.javalin.springdata.SpringDataPlugin;
import sparkles.support.javalin.testing.JavalinTestRunner;
import sparkles.support.javalin.testing.HttpClient;
import sparkles.support.javalin.testing.TestApp;
import sparkles.support.javalin.testing.TestClient;
import lombok.extern.slf4j.Slf4j;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JavalinTestRunner.class)
@Slf4j
public class ReplicaTest {
  static {
    System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX + "sparkles", "debug");
  }

  private static DataSource createDataSource() {
    final DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setDriverClassName(Environment.value("JDBC_DRIVER", "org.sqlite.JDBC"));
    ds.setUrl(Environment.value("JDBC_URL", "jdbc:sqlite:tmp/sqlite/replica-test.db"));
    ds.setUsername(Environment.value("JDBC_USER", ""));
    ds.setPassword(Environment.value("JDBC_PASSWORD", ""));

    return ds;
  }

  private static Map<String, Object> createHibernateProperties(DataSource dataSource) {
    return Collections.<String, Object> newMap()
      .put("javax.persistence.nonJtaDataSource", dataSource)
      .put("javax.persistence.transactionType", "RESOURCE_LOCAL")
      .put("hibernate.show_sql", true || Environment.isDevelop())
      .put("hibernate.format_sql", true)
      .put("hibernate.hbm2ddl.auto", "create")
      .put("hibernate.dialect", Environment.value("HIBERNATE_DIALECT", "org.hibernate.dialect.SQLiteDialect"))
      .build();
  }

  @TestApp
  private Javalin app = Javalin.create(cfg -> {
    final DataSource ds = createDataSource();

    cfg.registerPlugin(FlywayPlugin.create(ds, "persistence/migration"));
    cfg.registerPlugin(SpringDataPlugin.create("replica", createHibernateProperties(ds)));

    cfg.registerPlugin(R.createPlugin());
    cfg.registerPlugin(new CollectionApi());
    cfg.registerPlugin(new DocumentApi());
    cfg.registerPlugin(new VersionApi());

    cfg.requestLogger((ctx, ms) -> {
      log.info("{} {} served in {} msec", ctx.method(), ctx.path(), ms);
    });
  }).get("/hello", ctx -> { ctx.status(204); });

  @TestClient
  private HttpClient client;

  @Test
  public void itCreates() {
    assertThat(app).isNotNull();
    assertThat(client).isNotNull();
    assertThat(client.get("/hello").send().code()).isEqualTo(204);
  }

  @Test
  public void collectionAndDocumentWalkthrough() {
    // POST: create collection
    client.post("/collection")
      .json("{\"name\":\"foo\"}")
      .send();
    assertThat(client.response().code()).isEqualTo(201);
    assertThat(client.response().header("Location")).isNotEmpty();

    // HEAD: collection should exist
    client.head(client.response().header("Location")).send();
    assertThat(client.response().code()).isEqualTo(204);

    // POST: create document
    client.post("/collection/foo/document")
      .json("{\"name\":\"John Doe\",\"age\":40 }")
      .send();
    assertThat(client.response().code()).isEqualTo(201);
    final String documentUrl = client.response().header("Location");
    assertThat(documentUrl).startsWith("collection/foo/");
    final JsonObject documentJson = client.responseBodyJson();
    assertThat(documentJson).isNotNull();
    final String documentId = JavaxJson.propertyString(documentJson, "/_id");
    assertThat(documentId).isNotEmpty();
    final String documentVersion = JavaxJson.propertyString(documentJson, "/_meta/version");
    assertThat(documentVersion).isNotEmpty();

    // HEAD: document should exist
    client.head(documentUrl).send();
    assertThat(client.response().code()).isEqualTo(204);

    // PUT: update document
    client.put("/collection/foo/document")
      .json("{\"name\":\"Alice\",\"_id\":\"" + documentId + "\",\"_meta\":{\"version\":\"" + documentVersion + "\"}}")
      .send();
    final JsonObject updatedDocument = client.responseBodyJson();
    assertThat(JavaxJson.propertyString(updatedDocument, "/_id")).isEqualTo(documentId);
    assertThat(JavaxJson.propertyString(updatedDocument, "/_meta/version")).isNotEqualTo(documentVersion);
    assertThat(client.response().code()).isEqualTo(200);
    assertThat(client.response().header("Location")).startsWith("collection/foo/document/");

    client.get(documentUrl).send();
    assertThat(client.response().code()).isEqualTo(200);
    assertThat(client.responseBodyString()).contains("\"name\":\"Alice\"");

    // GET: conditional If-Modified-Since
    client.get(documentUrl)
      .header("If-Modified-Since", JavaxJson.propertyString(updatedDocument, "/_meta/lastModified"))
      .send();
    assertThat(client.response().code()).isEqualTo(304);

    // HEAD: conditional If-None-Match
    client.head(documentUrl)
      .header("If-None-Match", JavaxJson.propertyString(updatedDocument, "/_meta/version"))
      .send();
    assertThat(client.response().code()).isEqualTo(304);


    // ... work in progress ...
    client.get("collection/foo/byVersion/" + documentId + "/" + documentVersion)
      .send();
    System.out.println(client.responseBodyString());
    assertThat(client.response().code()).isEqualTo(501);
  }

}
