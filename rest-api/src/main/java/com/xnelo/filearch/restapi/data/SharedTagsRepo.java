package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.jooq.tables.SharedTags;
import com.xnelo.filearch.jooq.tables.records.SharedTagsRecord;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@Slf4j
@RequestScoped
public class SharedTagsRepo {
  private final DSLContext context;

  @Inject
  public SharedTagsRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();
  }

  public Uni<Boolean> addSharedTag(final Long tagId, final Long groupId) {
    SharedTagsRecord sharedTag = new SharedTagsRecord(tagId, groupId);
    return Uni.createFrom()
        .item(context.insertInto(SharedTags.SHARED_TAGS).set(sharedTag).execute())
        .map(res -> res == 1)
        .onFailure()
        .invoke(ex -> log.error("Error adding shared tagid:{} groupId:{}", tagId, groupId, ex))
        .onFailure()
        .recoverWithItem(Boolean.FALSE);
  }
}
