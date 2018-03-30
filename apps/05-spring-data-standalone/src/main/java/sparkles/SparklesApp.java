package sparkles;

import com.squareup.moshi.Moshi;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.hsqldb.jdbc.JDBCDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;
import sparkles.drinks.Drink;
import sparkles.drinks.DrinkRepository;

import static spark.Spark.*;
import static sparkles.support.moshi.MoshiResponseTransformer.moshiTransformer;
import static sparkles.support.persistence.migrations.MigrationSupport.runMigrations;
import static sparkles.support.persistence.SpringDataSupport.initPersistence;
import static sparkles.support.persistence.SpringDataSupport.repository;

public class SparklesApp implements SparkApplication {
  static {
    System.setProperty(org.slf4j.impl.SimpleLogger.LOG_KEY_PREFIX + "sparkles", Environment.logLevel());
  }

  private static final Logger LOG = LoggerFactory.getLogger(SparklesApp.class);

  public static void main(String[] args) {
    new SparklesApp().init();
  }

  @Override
  public void init() {
    LOG.info("SparklesApp running in {} environment.", Environment.environment());

    LOG.debug("Initializing persistence layer...");
    DataSource dataSource = createDataSource();
    runMigrations(dataSource, "persistence/migrations/flyway");
    EntityManagerFactory factory = Persistence.createEntityManagerFactory("drinks", createHibernateProperties(dataSource));
    initPersistence(factory);

    LOG.debug("Initializing routes...");
    get("/", (req, res) -> {
      final DrinkRepository repository = repository(req, DrinkRepository.class);

      return repository.findAll();
    }, moshiTransformer(List.class));

    get("/:id", (req, res) -> {
      final DrinkRepository repository = repository(req, DrinkRepository.class);

      return repository
        .findById(UUID.fromString(req.params("id")))
        .orElseThrow(() -> new RuntimeException("Not Found"));
    }, moshiTransformer(Drink.class));

    post("/", (req, res) -> {
      LOG.debug("Request body: {}", req.body());
      final DrinkRepository repository = repository(req, DrinkRepository.class);

      return repository.save(new Drink().withName("foo"));
    }, moshiTransformer(Drink.class));
  }


  public static DataSource createDataSource() {
    JDBCDataSource ds = new JDBCDataSource();
    ds.setUrl("jdbc:hsqldb:mem:standalone");
    ds.setUser("sa");
    ds.setPassword("");

    return ds;
  }

  public static Map<String, Object> createHibernateProperties(DataSource dataSource) {
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", dataSource);
    props.put("javax.persistence.transactionType", "RESOURCE_LOCAL");
    props.put("hibernate.show_sql", true);
    props.put("hibernate.format_sql", false);
    props.put("hibernate.hbm2ddl.auto", "validate");
    props.put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");

    return props;
  }

}
