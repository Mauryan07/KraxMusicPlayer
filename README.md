# KraxMusicPlayer (backend)

Simple Java backend for a music player. Provides a REST API for playback, playlists and library management — any frontend can be used with it.

Prerequisites
- Java 11+

Quick start
1. Clone
   git clone https://github.com/Mauryan07/KraxMusicPlayer.git
   cd KraxMusicPlayer
2. Build
   mvn clean package
3. Run
   java -jar build/libs/*.jar
   or
   java -jar target/*.jar
4. Open your frontend and point API requests to http://localhost:8080 (or the PORT you set).

Configuration
- Configure via environment variables or application.properties as needed (PORT, DB connection, etc.).

Notes
- This repository is the backend only — it exposes REST endpoints for any frontend to consume.
- Keep the frontend separate; call the backend API endpoints to control playback and manage data.
