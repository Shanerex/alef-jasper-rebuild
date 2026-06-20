# api — Spring Boot + Spring AI

Empty on purpose. The Dev agent scaffolds this per `specs/03-tasks.md`.
Runs in two Spring profiles from one codebase:
  - profile `api`    : web API + concierge (ChatClient + @Tool beans + advisors) + RAG
  - profile `worker` : Redis Streams consumer (lead.created, kb.changed)
Stack is binding per `specs/02-architecture.md`. Do not deviate without an ADR.
