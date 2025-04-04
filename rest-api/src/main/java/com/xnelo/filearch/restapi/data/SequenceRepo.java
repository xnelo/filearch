package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.jooq.Sequences;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@RequestScoped
public class SequenceRepo {
  private final DSLContext context;

  @Inject
  public SequenceRepo(AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
  }

  public Uni<Long> getNextFileUploadNumber() {
    return Uni.createFrom().item(context.nextval(Sequences.SEQ_FILE_UPLOAD_NUMBER));
  }
}
