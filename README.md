# todo-snap

## Description

An example REST API for managing todos.

To run locally, you'll need an instance of postgres. You can bring one up with docker by using the command:

```sh
docker-compose up -d
```

on linux (with an older docker installation), or:

```sh
docker compose up -d
```

on a mac (with a newer docker installation). Then follow the 'developing' heading from the duct readme below.

## Routes

| name     | route                   | description                                                                         |
| -------- | ----------------------- | ----------------------------------------------------------------------------------- |
| list     | GET "/todos/{email}"    | Returns a list of todos for a given user                                            |
| create   | POST "/todos"           | Creates a new incomplete todo with a given title for a given user                   |
| update   | PUT "/todos"            | Updates the title or completed status for a specific todo belonging to a given user |
| delete   | DELETE "/todos"         | Deletes a todo matching an id/email pair (using an improper delete body)            |
| summary  | GET "/summary/{email}"  | Returns complete/incomplete todo counts for a given user                            |
| burndown | GET "/burndown/{email}" | Returns an array of todo events with a running burndown total of incomplete todos   |

The json for a single returned todo looks like:

```json
{
  "id": "a45fe0de-6707-4de2-804f-6527feb90fb6",
  "title": "bake cookies",
  "complete": false
}
```

The json returned by the summary endpoint looks like:

```json
{
  "complete": 10,
  "incomplete": 20
}
```

The json returned by the burndown endpoint looks like:

TODO

## Things I'd like to fix

1. Unit tests
   They're good. This doesn't have them.

2. Integration tests
   I was hoping duct would help with this. I'm guessing there's nice patterns for doing it, but judging from github issues,
   it seems like I'd have to ask for advice in the clojurians slack, which didn't seem appropriate for this exercise.

3. Structured documentation
   I'd like to try out reitit and its swagger integration.

---

# Generated Duct README

## Developing

### Setup

When you first clone this repository, run:

```sh
lein duct setup
```

This will create files for local configuration, and prep your system
for the project.

### Environment

To begin developing, start with a REPL.

```sh
lein repl
```

Then load the development environment.

```clojure
user=> (dev)
:loaded
```

Run `go` to prep and initiate the system.

```clojure
dev=> (go)
:duct.server.http.jetty/starting-server {:port 3000}
:initiated
```

By default this creates a web server at <http://localhost:3000>.

When you make changes to your source files, use `reset` to reload any
modified files and reset the server.

```clojure
dev=> (reset)
:reloading (...)
:resumed
```

### Testing

Testing is fastest through the REPL, as you avoid environment startup
time.

```clojure
dev=> (test)
...
```

But you can also run tests through Leiningen.

```sh
lein test
```

## Legal

Copyright © 2021 FIXME
