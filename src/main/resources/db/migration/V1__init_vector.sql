-- Увімкнення розширення pgvector для збереження та пошуку векторних ембедингів (потрібно для RAG)
CREATE EXTENSION IF NOT EXISTS vector;

-- Створення базової таблиці для зберігання документів бази знань (наприклад, патчів, статей тощо)
CREATE TABLE knowledge_documents (
                                     id SERIAL PRIMARY KEY,
                                     title VARCHAR(255) NOT NULL,
                                     content TEXT NOT NULL,
                                     embedding vector(1536) -- 1536 розмірність вектором для стандартних моделей OpenAI (наприклад, text-embedding-3-small)
);