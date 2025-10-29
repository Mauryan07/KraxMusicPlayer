# KraxMusicPlayer (backend)

Minimal Java Spring Boot backend for a music player. Exposes REST endpoints for playback, playlists and library management — any frontend can consume it.

Prerequisites
- JDK 21
- Maven

Quick start
1. Clone
   git clone https://github.com/Mauryan07/KraxMusicPlayer.git
   cd KraxMusicPlayer
2. Build
   mvn clean package
3. Run
   mvn spring-boot:run
   or
   java -jar target/*.jar
4. Default API base: http://localhost:8080 (set PORT in application.properties or via environment)

Configuration
- Edit src/main/resources/application.properties or set environment variables (DB, PORT, etc.).

Notes
- This is backend-only (Spring Boot). Use any frontend to call its REST API.
