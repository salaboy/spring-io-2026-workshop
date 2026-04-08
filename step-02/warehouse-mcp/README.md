# Warehouse MCP Server Spring Boot Application


## Running with Reuse
Env Var tells Testcontainers to reuse containers if they are already running.
The `--reuse=true` flag is passed to the Spring Boot test runner we can define what is reused from the application point of view.

```shell
TESTCONTAINERS_REUSE_ENABLE=true mvn -Dspring-boot.run.arguments="--reuse=true" spring-boot:test-run
```