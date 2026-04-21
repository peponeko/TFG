@echo off
set ANTHROPIC_BASE_URL=http://127.0.0.1:11434/v1
set ANTHROPIC_API_KEY=sk-ant-local
"C:\Users\Pedro\.local\bin\claude.exe" --model ollama/qwen2.5-coder:latest
pause