#!/usr/bin/env sh
# Modelos locais do pipeline: embedding BGE-M3 (1024 dims) e o braço Gemma.
# Ajuste OLLAMA_CHAT_MODEL no .env se a tag do Gemma diferir na sua instalação
# (ex.: gemma3n:e4b enquanto gemma4:e4b não estiver publicado no registry).
set -e
ollama pull bge-m3
ollama pull "${OLLAMA_CHAT_MODEL:-gemma4:e4b}"
