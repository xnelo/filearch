package com.xnelo.filearch.common.testfixtures.testdata;

import com.xnelo.filearch.common.testfixtures.testdata.data.ArtifactsTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.FileTagsTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.FoldersTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.GroupItemsTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.GroupMembersTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.GroupsTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.SharedTagsTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.StoredFilesTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.TagsTestData;
import com.xnelo.filearch.common.testfixtures.testdata.data.UserTestData;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

public class DatabaseTestData {
  private final DataSource dataSource;
  private final String schema;
  private final DSLContext dslContext;

  public DatabaseTestData(DataSource dataSource, final String schema) {
    this.dataSource = dataSource;
    this.schema = schema;
    this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES);
    dslContext.setSchema(schema).execute();
  }

  public void cleanDatabase() {
    ArtifactsTestData.clearDatabaseTable(dslContext);
    FileTagsTestData.clearDatabaseTable(dslContext);
    FoldersTestData.clearDatabaseTable(dslContext);
    GroupItemsTestData.clearDatabaseTable(dslContext);
    GroupMembersTestData.clearDatabaseTable(dslContext);
    GroupsTestData.clearDatabaseTable(dslContext);
    SharedTagsTestData.clearDatabaseTable(dslContext);
    StoredFilesTestData.clearDatabaseTable(dslContext);
    TagsTestData.clearDatabaseTable(dslContext);
    UserTestData.clearDatabaseTable(dslContext);
  }

  public void loadDataIntoDatabase() {
    ArtifactsTestData.loadTestData(dslContext);
    FileTagsTestData.loadTestData(dslContext);
    FoldersTestData.loadTestData(dslContext);
    GroupItemsTestData.loadTestData(dslContext);
    GroupMembersTestData.loadTestData(dslContext);
    GroupsTestData.loadTestData(dslContext);
    SharedTagsTestData.loadTestData(dslContext);
    StoredFilesTestData.loadTestData(dslContext);
    TagsTestData.loadTestData(dslContext);
    UserTestData.loadTestData(dslContext);
  }
}
