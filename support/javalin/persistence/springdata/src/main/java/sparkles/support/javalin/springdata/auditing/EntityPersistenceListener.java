package sparkles.support.javalin.springdata.auditing;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.util.Assert;

/**
 * Adapter for connecting Spring's auditing infrastructure to Hibernate in standalone usage.
 *
 * Since most of the related Spring APIs are either exernal or hidden behind AspectJ, they are
 * re-implemented here for feature parity.
 *
 * @link https://github.com/spring-projects/spring-data-jpa/blob/master/src/main/java/org/springframework/data/jpa/domain/support/EntityPersistenceListener.java
 * @link https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/auditing/AuditingHandler.java#L160-L176
 * @link https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/auditing/DefaultAuditableBeanWrapperFactory.java#L57-L75
 * @link https://github.com/spring-projects/spring-data-commons/blob/master/src/main/java/org/springframework/data/auditing/AnnotationAuditingMetadata.java#L38-L47
 */
public class EntityPersistenceListener {

  @PrePersist
  public void touchForCreate(Object target) {
    Assert.notNull(target, "Entity must not be null!");

    ContextAware context = Auditing.getStrategy().resolveCurrentContext();
    AuditingHandler handler = context.getHandler();
    if (handler != null) {
      handler.setAuditorAware(context);
      handler.markCreated(target);
    }
  }

  @PreUpdate
  public void touchForUpdate(Object target) {
    Assert.notNull(target, "Entity must not be null!");

    ContextAware context = Auditing.getStrategy().resolveCurrentContext();
    AuditingHandler handler = context.getHandler();
    if (handler != null) {
      handler.setAuditorAware(context);
      handler.markModified(target);
    }
  }

}
