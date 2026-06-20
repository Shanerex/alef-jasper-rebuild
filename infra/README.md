# infra — local stack support

Init SQL, Qdrant bootstrap, and the Ollama model pull live here.
The datastores and Ollama can run before any app code exists:
  make infra-up   # postgres, qdrant, redis, ollama only
  make pull-models
