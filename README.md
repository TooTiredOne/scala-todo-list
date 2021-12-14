# todo-tapir-http4s-doobie

## Description

Simple rest-api HTTP server for todo-list service based on tapir, http4s, and doobie. PostgreSQL is used for storage.

User can register using `username/password`. The rest of the service is available only for the authorized users. 
The authorization is done using `basic auth`. 

An authorized user can add new tasks with `description` and `is_finished` mark, change the attributes of
his/her existing tasks, delete tasks, and get added tasks applying filters by current task `status` and/or `substring match` 
in description.

## How to start project

There are 2 options:
- start by running `docker-compose up` (notice that the postgres container will try to create a volume in path `${HOME}/postgres-data/postgres/`, you may want to change the path in `docker-compose.yml` or just remove the volume)
- start locally (in this case, don't forget to change the database address in `src/main/resources/application.conf`)

## How to use project

The easiest way to do so is by accessing the `Swagger UI` in `http://localhost:8080/docs/`.

Alternatively, the endpoints may be found in `AuthController` and `TaskController` files.

## Tests

The project is covered with simple unit test. They can be run locally using `sbt test`.
## References:
- [todo-http4s-doobie](https://github.com/jaspervz/todo-http4s-doobie)
- [tapir-http4s-todo-mvc](https://github.com/hejfelix/tapir-http4s-todo-mvc)
- [getting started with scala, sbt, and docker from scratch](https://yuchen52.medium.com/getting-started-with-docker-scala-sbt-d91f8ac22f5f)
- [tapir](https://tapir.softwaremill.com/en/latest/)
- [doobie](https://tpolecat.github.io/doobie/)
- [http4s](https://http4s.org/)
- [circe](https://circe.github.io/circe/)
- [scalamock](https://scalamock.org/)
