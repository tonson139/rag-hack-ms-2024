# Music recommending Application (RAG_Hack)

> Discover your next favorite song with our personalized music recommendation system. Easily search for songs by lyrics, explore artist information, and get tailored suggestions based on your preferences. Let us help you find the perfect soundtrack for your life.

---
<div align="center">
<img src="asset/demo-live.gif" />
</div>

## RAG_Hack
-  https://github.com/microsoft/RAG_Hack/tree/main
## Features
- Data set from https://www.kaggle.com/datasets/saurabhshahane/music-dataset-1950-to-2019
- Search for song from lyrics
- Find information about artist name, release data, genre, topic of the song.
- Ask for suggestion of the song based on your preferences.

## Application Architecture
- Powered by **llama:3.1b**

**Application**
```mermaid
graph TD
    FE[React frontend client] -- (1a) Prompt --> BE[Springboot application]
    BE -- (1b) --> FE
    BE -- (2a) Retrieve --> DB[PostgresDB + PGVector]
    DB -- (2b) --> BE
    BE -- (3a) Generate --> LLM[LLM - llama:3.1b]
    LLM -- (3b) --> BE
```
**Data pipeline**
```mermaid
graph LR
PY[Python pipeline] -- (0) embedding and insert data --> DB
```

<div align="center">
<img src="asset/demo-snapshot.png" />
</div>

## Tech Stack

The tech stack for this application includes: React, Java Spring Boot, Postgresql with Pgvector.

### Team member
-  Application by [Patanin](https://github.com/tonson139)
-  Data pipeline by [Worachit](https://github.com/worachit)
