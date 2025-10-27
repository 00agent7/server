# Testing the Mafia Game API

This guide provides instructions on how to test the Mafia Game API with real API calls. It includes steps to set up the environment, start the application, and make API calls to test the functionality.

## Setting Up the Environment

### Option 1: Using Docker Compose (Recommended)

The easiest way to set up the environment is using Docker Compose, which will start both the application and the PostgreSQL database.

1. Make sure you have Docker and Docker Compose installed on your system.
2. Navigate to the project root directory.
3. Run the following command:

```bash
docker-compose up -d
```

This will start the application on port 8080 and the PostgreSQL database on port 5432.

### Option 2: Using Maven

If you prefer to run the application directly without Docker:

1. Make sure you have PostgreSQL installed and running on your system.
2. Create a database named `mafia` with username `mafia_user` and password `mafia_password`.
3. Navigate to the project root directory.
4. Run the following command:

```bash
./mvnw spring-boot:run
```

Or on Windows:

```bash
mvnw.cmd spring-boot:run
```

### Option 3: Using an IDE

1. Make sure you have PostgreSQL installed and running on your system.
2. Create a database named `mafia` with username `mafia_user` and password `mafia_password`.
3. Open the project in your IDE (IntelliJ IDEA, Eclipse, etc.).
4. Run the `MafiaApplication.java` file as a Java application.

## Testing the API

You can test the API using tools like Postman, curl, or any HTTP client. The application includes a Postman collection (`Mafia_Game_API.postman_collection.json`) that you can import into Postman for easy testing.

### Importing the Postman Collection

1. Open Postman.
2. Click on "Import" in the top left corner.
3. Select the `Mafia_Game_API.postman_collection.json` file from the project root directory.
4. The collection will be imported with all the API endpoints ready to use.

### Basic Game Flow

Here's a step-by-step guide to test the main game flow using the API:

#### 1. Create a New Game

**Request:**
```
POST http://localhost:8080/api/mafia/games
```

**Response:**
```json
{
    "gameId": "game123"
}
```

Save the `gameId` as you'll need it for subsequent requests.

#### 2. Add Players to the Game

Add at least 10 players to the game (for a standard Mafia game).

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/players?playerId=player1&seatNumber=1
```

Replace `{gameId}` with the ID from step 1. Repeat this request for each player, incrementing the `playerId` and `seatNumber` values.

**Response:**
```json
{
    "playerId": "player1",
    "seatNumber": 1
}
```

#### 3. Assign Roles

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/assign-roles
```

**Response:**
```json
{
    "gameId": "{gameId}",
    "status": "roles_assigned"
}
```

#### 4. Start the Game

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/start
```

**Response:**
```json
{
    "gameId": "{gameId}",
    "status": "started"
}
```

#### 5. Get Game State

**Request:**
```
GET http://localhost:8080/api/mafia/games/{gameId}/state
```

**Response:**
```json
{
    "gameId": "{gameId}",
    "phase": "DAY",
    "status": "ACTIVE",
    "dayCount": 1,
    "nightCount": 0,
    "players": [
        {
            "seatNumber": 1,
            "alive": true,
            "masked": false,
            "sanctions": {
                "warningCount": 0,
                "removed": false,
                "yellowCard": false,
                "redCard": false,
                "lostNextSpeech": false
            }
        }
    ],
    "additionalPlayers": "..."
}
```

#### 6. Get Player Role

Each player can check their role:

**Request:**
```
GET http://localhost:8080/api/mafia/games/{gameId}/players/{playerId}/role
```

**Response:**
```json
{
    "playerId": "{playerId}",
    "seatNumber": 1,
    "role": "CIVILIAN",
    "isRed": true,
    "isBlack": false
}
```

#### 7. Day Phase Actions

During the day phase, players can make nominations:

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/nominations?nominatorSeat=1&nomineeSeat=2
```

**Response:**
```json
{
    "nominatorSeat": 1,
    "nomineeSeat": 2
}
```

Players can vote for nominations:

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/nominations/{nominationIndex}/votes?voterSeat=3
```

Replace `{nominationIndex}` with the index of the nomination (usually 0 for the first nomination).

**Response:**
```json
{
    "status": "voted"
}
```

#### 8. Move to Night Phase

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/next-phase
```

**Response:**
```json
{
    "gameId": "{gameId}",
    "phase": "NIGHT"
}
```

#### 9. Night Phase Actions

During the night phase, the Don can check if a player is the Sheriff:

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/check-sheriff?checkerSeat=2&targetSeat=5
```

Replace `checkerSeat` with the Don's seat number and `targetSeat` with the player to check.

**Response:**
```json
{
    "isSheriff": true
}
```

The Sheriff can check if a player is Mafia:

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/check-mafia?checkerSeat=5&targetSeat=2
```

Replace `checkerSeat` with the Sheriff's seat number and `targetSeat` with the player to check.

**Response:**
```json
{
    "isMafia": true
}
```

The Mafia can kill a player:

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/mafia-kill?mafiaSeats=2&mafiaSeats=7&mafiaSeats=9&targetSeat=5
```

Replace `mafiaSeats` with the seat numbers of all Mafia players and `targetSeat` with the player to kill.

**Response:**
```json
{
    "status": "killed"
}
```

#### 10. Move Back to Day Phase

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/next-phase
```

**Response:**
```json
{
    "gameId": "{gameId}",
    "phase": "DAY"
}
```

#### 11. End the Game

**Request:**
```
POST http://localhost:8080/api/mafia/games/{gameId}/end
```

**Response:**
```json
{
    "status": "ended"
}
```

### Additional API Endpoints

The API includes many more endpoints for various game actions:

- **Chat Messages**: Send public and private messages between players
- **Player Sanctions**: Add warnings, issue yellow/red cards, remove players
- **AI Agents**: Create and manage AI agents in the game
- **Prima Nota**: Record guesses about Mafia members

Refer to the Postman collection for a complete list of available endpoints and their parameters.

## Monitoring

While testing, you can monitor the server logs in the terminal where the server is running to diagnose any issues that might occur.

## Troubleshooting

- If you encounter connection issues with the database, make sure PostgreSQL is running and the database credentials are correct.
- If the API returns 404 errors, check that you're using the correct endpoint paths as defined in the controller.
- If you get 400 Bad Request errors, check that you're providing all required parameters and they have valid values.
