# filearch
A file archiving application that allows tagging and searching.

# Adding DB Migration
1) Create the new migration script in the `filearch-db-migration/migrations/` folder.
2) Use Flyway to migrate
   1) Desktop
      1) Open Desktop application and log in
      2) Open the `filearch-db-migration/flyway.toml` project file.
      3) Connect to database `Xnelo Media DB`
      4) click `Run Migrate` button
   2) Command-line~~~~
      1) TODO
3) Update JOOQ project
   1) cd into jooq folder
   2) execute `../gradlew generatejooq`
      1) You need to set the environment variable `FILEARCH_DB_HOST_ADDRESS` (it defaults to localhost). You
         can do this in a single command 
         ```
         powershell -Command { $env:FILEARCH_DB_HOST_ADDRESS="<DB_ADDRESS>"; ../gradlew generatejooq }
         ```