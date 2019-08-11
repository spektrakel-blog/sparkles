package sparkles.entity;

import io.javalin.Extension;
import io.javalin.Javalin;
import java.util.UUID;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import sparkles.support.javalin.JavalinApp;
import sparkles.support.javalin.keycloak.security.KeycloakRoles;
import sparkles.support.javalin.spring.data.auditing.Auditing;

import static io.javalin.apibuilder.ApiBuilder.crud;
import static sparkles.support.javalin.JavalinApp.requires;
import static sparkles.support.javalin.spring.data.SpringDataExtension.springData;
import static sparkles.support.javalin.spring.data.crud.CrudRepositoryHandler.crudHandler;

@Slf4j
public class FooApi implements Extension {

  @Override
  public void registerOnJavalin (Javalin app) {

    app.get("/", (ctx) -> {
      FooRepository repository = springData(ctx).createRepository(FooRepository.class);

      List<FooEntity> stuffs = repository.findAll();
      for (FooEntity stuff : stuffs) {
        log.info("stuff is {} // {}", stuff.createdAt, stuff.createdBy);
      }

      ctx.result("count: " + stuffs.size());
    }, requires(KeycloakRoles.ANYONE))
    .post("/", (ctx) -> {
      Object auditor = Auditing.getStrategy().resolveCurrentContext().getCurrentAuditor().get();
      log.info("Current auditor is {}", auditor);

      FooEntity e = new FooEntity().withName("foobararar").addKind(FooEntity.Kind.FOO);
      if (Math.random() < 0.5) {
        e.addKind(FooEntity.Kind.FOOBAR);
      }

      FooRepository repository = springData(ctx).createRepository(FooRepository.class);
      FooEntity entity = repository.save(e);

      ctx.json(entity).status(201);
    })
    .routes(() -> {
      crud("stuff/:id", crudHandler(FooRepository.class, FooEntity.class, UUID::fromString));
    });

  }
}
