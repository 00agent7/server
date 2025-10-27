package org.example.mafia.controller;

import org.example.mafia.model.Agent;
import org.example.mafia.model.AgentTool;
import org.example.mafia.model.ChatMessage;
import org.example.mafia.model.Game;
import org.example.mafia.model.Nomination;
import org.example.mafia.model.Player;
import org.example.mafia.model.PlayerPoints;
import org.example.mafia.model.PlayerSanctions;
import org.example.mafia.model.enums.GamePhase;
import org.example.mafia.model.enums.GameStatus;
import org.example.mafia.model.enums.PlayerRole;
import org.example.mafia.service.AgentService;
import org.example.mafia.service.ChatService;
import org.example.mafia.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for Mafia game operations.
 * This controller exposes the game functionality to AI agents through a REST API.
 */
@RestController
@RequestMapping("/api/mafia")
public class MafiaGameController {

    private final GameService gameService;
    private final AgentService agentService;
    private final ChatService chatService;

    @Autowired
    public MafiaGameController(GameService gameService, AgentService agentService, ChatService chatService) {
        this.gameService = gameService;
        this.agentService = agentService;
        this.chatService = chatService;
    }

    /**
     * Creates a new game.
     * 
     * @return The created game ID
     */
    @PostMapping("/games")
    public ResponseEntity<Map<String, String>> createGame() {
        Game game = gameService.createGame();
        Map<String, String> response = new HashMap<>();
        response.put("gameId", game.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a game by ID.
     * 
     * @param gameId The ID of the game to get
     * @return The game details
     */
    @GetMapping("/games/{gameId}")
    public ResponseEntity<Map<String, Object>> getGame(@PathVariable String gameId) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("gameId", game.getId());
        response.put("phase", game.getCurrentPhase());
        response.put("status", game.getStatus());
        response.put("dayCount", game.getDayCount());
        response.put("nightCount", game.getNightCount());
        response.put("playerCount", game.getPlayers().size());
        response.put("alivePlayerCount", game.getAlivePlayers().size());

        return ResponseEntity.ok(response);
    }

    /**
     * Adds a player to a game.
     * 
     * @param gameId The ID of the game to add the player to
     * @param playerId The ID of the player to add
     * @param seatNumber The seat number for the player
     * @return The added player details
     */
    @PostMapping("/games/{gameId}/players")
    public ResponseEntity<Map<String, Object>> addPlayer(
            @PathVariable String gameId,
            @RequestParam String playerId,
            @RequestParam int seatNumber) {

        Player player = gameService.addPlayer(gameId, playerId, seatNumber);
        if (player == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("playerId", player.getId());
        response.put("seatNumber", player.getSeatNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Assigns roles to players after all players are added.
     * 
     * @param gameId The ID of the game
     * @return Success status
     */
    @PostMapping("/games/{gameId}/assign-roles")
    public ResponseEntity<Map<String, Object>> assignRoles(@PathVariable String gameId) {
        boolean assigned = gameService.assignRoles(gameId);
        if (!assigned) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("gameId", gameId);
        response.put("status", "roles_assigned");
        return ResponseEntity.ok(response);
    }

    /**
     * Starts a game.
     * 
     * @param gameId The ID of the game to start
     * @return Success status
     */
    @PostMapping("/games/{gameId}/start")
    public ResponseEntity<Map<String, Object>> startGame(@PathVariable String gameId) {
        boolean started = gameService.startGame(gameId);
        if (!started) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("gameId", gameId);
        response.put("status", "started");
        return ResponseEntity.ok(response);
    }

    /**
     * Advances a game to the next phase.
     * 
     * @param gameId The ID of the game to advance
     * @return The new phase
     */
    @PostMapping("/games/{gameId}/next-phase")
    public ResponseEntity<Map<String, Object>> nextPhase(@PathVariable String gameId) {
        GamePhase phase = gameService.nextPhase(gameId);
        if (phase == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("gameId", gameId);
        response.put("phase", phase);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a nomination during the Day phase.
     * 
     * @param gameId The ID of the game
     * @param nominatorSeat The seat number of the player making the nomination
     * @param nomineeSeat The seat number of the player being nominated
     * @return The created nomination details
     */
    @PostMapping("/games/{gameId}/nominations")
    public ResponseEntity<Map<String, Object>> createNomination(
            @PathVariable String gameId,
            @RequestParam int nominatorSeat,
            @RequestParam int nomineeSeat) {

        Nomination nomination = gameService.createNomination(gameId, nominatorSeat, nomineeSeat);
        if (nomination == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("nominatorSeat", nomination.getNominator().getSeatNumber());
        response.put("nomineeSeat", nomination.getNominee().getSeatNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraws a nomination.
     * 
     * @param gameId The ID of the game
     * @param nominationIndex The index of the nomination to withdraw
     * @param playerSeat The seat number of the player attempting to withdraw the nomination
     * @return Success status
     */
    @PostMapping("/games/{gameId}/nominations/{nominationIndex}/withdraw")
    public ResponseEntity<Map<String, Object>> withdrawNomination(
            @PathVariable String gameId,
            @PathVariable int nominationIndex,
            @RequestParam int playerSeat) {

        boolean withdrawn = gameService.withdrawNomination(gameId, nominationIndex, playerSeat);
        if (!withdrawn) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "withdrawn");
        return ResponseEntity.ok(response);
    }

    /**
     * Votes for a nomination.
     * 
     * @param gameId The ID of the game
     * @param nominationIndex The index of the nomination to vote for
     * @param voterSeat The seat number of the player voting
     * @return Success status
     */
    @PostMapping("/games/{gameId}/nominations/{nominationIndex}/votes")
    public ResponseEntity<Map<String, Object>> vote(
            @PathVariable String gameId,
            @PathVariable int nominationIndex,
            @RequestParam int voterSeat) {

        boolean voted = gameService.vote(gameId, nominationIndex, voterSeat);
        if (!voted) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "voted");
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a player is the Sheriff.
     * This is used by the Don during the Night phase.
     * 
     * @param gameId The ID of the game
     * @param checkerSeat The seat number of the player doing the checking (must be the Don)
     * @param targetSeat The seat number of the player being checked
     * @return The check result
     */
    @PostMapping("/games/{gameId}/check-sheriff")
    public ResponseEntity<Map<String, Object>> checkSheriff(
            @PathVariable String gameId,
            @RequestParam int checkerSeat,
            @RequestParam int targetSeat) {

        Boolean isSheriff = gameService.checkSheriff(gameId, checkerSeat, targetSeat);
        if (isSheriff == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isSheriff", isSheriff);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks if a player is Mafia.
     * This is used by the Sheriff during the Night phase.
     * 
     * @param gameId The ID of the game
     * @param checkerSeat The seat number of the player doing the checking (must be the Sheriff)
     * @param targetSeat The seat number of the player being checked
     * @return The check result
     */
    @PostMapping("/games/{gameId}/check-mafia")
    public ResponseEntity<Map<String, Object>> checkMafia(
            @PathVariable String gameId,
            @RequestParam int checkerSeat,
            @RequestParam int targetSeat) {

        Boolean isMafia = gameService.checkMafia(gameId, checkerSeat, targetSeat);
        if (isMafia == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isMafia", isMafia);
        return ResponseEntity.ok(response);
    }

    /**
     * Performs a Mafia kill.
     * This is used by the Mafia during the Night phase.
     * 
     * @param gameId The ID of the game
     * @param mafiaSeats The seat numbers of the Mafia players
     * @param targetSeat The seat number of the player being targeted
     * @return Success status
     */
    @PostMapping("/games/{gameId}/mafia-kill")
    public ResponseEntity<Map<String, Object>> mafiaKill(
            @PathVariable String gameId,
            @RequestParam List<Integer> mafiaSeats,
            @RequestParam int targetSeat) {

        boolean killed = gameService.mafiaKill(gameId, mafiaSeats, targetSeat);
        if (!killed) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "killed");
        return ResponseEntity.ok(response);
    }

    /**
     * Records Prima Nota guesses.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player making the guesses
     * @param guesses The seat numbers of the guessed Mafia members
     * @return Success status
     */
    @PostMapping("/games/{gameId}/prima-nota")
    public ResponseEntity<Map<String, Object>> recordPrimaNotaGuesses(
            @PathVariable String gameId,
            @RequestParam int playerSeat,
            @RequestParam int[] guesses) {

        boolean recorded = gameService.recordPrimaNotaGuesses(gameId, playerSeat, guesses);
        if (!recorded) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "recorded");
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the player's role in a game.
     * This is used by AI agents to determine their role.
     * 
     * @param gameId The ID of the game
     * @param playerId The ID of the player
     * @return The player's role
     */
    @GetMapping("/games/{gameId}/players/{playerId}/role")
    public ResponseEntity<Map<String, Object>> getPlayerRole(
            @PathVariable String gameId,
            @PathVariable String playerId) {

        Game game = gameService.getGame(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        Player player = game.getPlayers().stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);

        if (player == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("playerId", player.getId());
        response.put("seatNumber", player.getSeatNumber());
        response.put("role", player.getRole());
        response.put("isRed", player.isRed());
        response.put("isBlack", player.isBlack());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets the current game state.
     * This provides a comprehensive view of the game state for AI agents.
     * 
     * @param gameId The ID of the game
     * @return The game state
     */
    @GetMapping("/games/{gameId}/state")
    public ResponseEntity<Map<String, Object>> getGameState(@PathVariable String gameId) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("gameId", game.getId());
        response.put("phase", game.getCurrentPhase());
        response.put("status", game.getStatus());
        response.put("dayCount", game.getDayCount());
        response.put("nightCount", game.getNightCount());

        // Include player information
        List<Map<String, Object>> playerInfo = game.getPlayers().stream()
                .map(p -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("seatNumber", p.getSeatNumber());
                    info.put("alive", p.isAlive());
                    info.put("masked", p.isMasked());

                    // Include sanctions information
                    PlayerSanctions sanctions = p.getSanctions();
                    if (sanctions != null) {
                        Map<String, Object> sanctionsInfo = new HashMap<>();
                        sanctionsInfo.put("warningCount", sanctions.getWarningCount());
                        sanctionsInfo.put("removed", sanctions.isRemoved());
                        sanctionsInfo.put("yellowCard", sanctions.hasYellowCard());
                        sanctionsInfo.put("redCard", sanctions.hasRedCard());
                        sanctionsInfo.put("lostNextSpeech", sanctions.hasLostNextSpeech());
                        info.put("sanctions", sanctionsInfo);
                    }

                    // Include points information if game is over
                    if (game.getStatus().isGameOver()) {
                        PlayerPoints points = p.getPoints();
                        if (points != null) {
                            Map<String, Object> pointsInfo = new HashMap<>();
                            pointsInfo.put("basePoints", points.getBasePoints());
                            pointsInfo.put("bonusPoints", points.getBonusPoints());
                            pointsInfo.put("penaltyPoints", points.getPenaltyPoints());
                            pointsInfo.put("compensationPoints", points.getCompensationPoints());
                            pointsInfo.put("totalPoints", points.getTotalPoints());
                            info.put("points", pointsInfo);
                        }
                    }

                    return info;
                })
                .collect(Collectors.toList());
        response.put("players", playerInfo);

        // Include nomination information if in Day or Voting phase
        if (game.getCurrentPhase() == GamePhase.DAY || game.getCurrentPhase() == GamePhase.VOTING) {
            List<Map<String, Object>> nominationInfo = game.getActiveNominations().stream()
                    .map(n -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("nominatorSeat", n.getNominator().getSeatNumber());
                        info.put("nomineeSeat", n.getNominee().getSeatNumber());
                        info.put("voteCount", n.getVoteCount());
                        return info;
                    })
                    .collect(Collectors.toList());
            response.put("nominations", nominationInfo);
        }

        // Include current speaker if in Day phase
        if (game.getCurrentPhase() == GamePhase.DAY && game.getCurrentSpeaker() != null) {
            response.put("currentSpeaker", game.getCurrentSpeaker().getSeatNumber());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Adds a warning to a player.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to warn
     * @return Success status
     */
    @PostMapping("/games/{gameId}/players/{playerSeat}/warnings")
    public ResponseEntity<Map<String, Object>> addWarning(
            @PathVariable String gameId,
            @PathVariable int playerSeat) {

        boolean warned = gameService.addWarning(gameId, playerSeat);
        if (!warned) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "warned");
        return ResponseEntity.ok(response);
    }

    /**
     * Removes a player from the game due to a serious violation.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to remove
     * @return Success status
     */
    @PostMapping("/games/{gameId}/players/{playerSeat}/remove")
    public ResponseEntity<Map<String, Object>> removePlayerForViolation(
            @PathVariable String gameId,
            @PathVariable int playerSeat) {

        boolean removed = gameService.removePlayerForViolation(gameId, playerSeat);
        if (!removed) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "removed");
        return ResponseEntity.ok(response);
    }

    /**
     * Issues a yellow card to a player.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to card
     * @return Success status
     */
    @PostMapping("/games/{gameId}/players/{playerSeat}/yellow-card")
    public ResponseEntity<Map<String, Object>> issueYellowCard(
            @PathVariable String gameId,
            @PathVariable int playerSeat) {

        boolean carded = gameService.issueYellowCard(gameId, playerSeat);
        if (!carded) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "yellow-carded");
        return ResponseEntity.ok(response);
    }

    /**
     * Issues a red card to a player.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to card
     * @return Success status
     */
    @PostMapping("/games/{gameId}/players/{playerSeat}/red-card")
    public ResponseEntity<Map<String, Object>> issueRedCard(
            @PathVariable String gameId,
            @PathVariable int playerSeat) {

        boolean carded = gameService.issueRedCard(gameId, playerSeat);
        if (!carded) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "red-carded");
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a player's points.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player
     * @return The player's points
     */
    @GetMapping("/games/{gameId}/players/{playerSeat}/points")
    public ResponseEntity<Map<String, Object>> getPlayerPoints(
            @PathVariable String gameId,
            @PathVariable int playerSeat) {

        PlayerPoints points = gameService.getPlayerPoints(gameId, playerSeat);
        if (points == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("basePoints", points.getBasePoints());
        response.put("bonusPoints", points.getBonusPoints());
        response.put("penaltyPoints", points.getPenaltyPoints());
        response.put("compensationPoints", points.getCompensationPoints());
        response.put("totalPoints", points.getTotalPoints());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets a player's sanctions.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player
     * @return The player's sanctions
     */
    @GetMapping("/games/{gameId}/players/{playerSeat}/sanctions")
    public ResponseEntity<Map<String, Object>> getPlayerSanctions(
            @PathVariable String gameId,
            @PathVariable int playerSeat) {

        PlayerSanctions sanctions = gameService.getPlayerSanctions(gameId, playerSeat);
        if (sanctions == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("warningCount", sanctions.getWarningCount());
        response.put("removed", sanctions.isRemoved());
        response.put("yellowCard", sanctions.hasYellowCard());
        response.put("redCard", sanctions.hasRedCard());
        response.put("lostNextSpeech", sanctions.hasLostNextSpeech());
        return ResponseEntity.ok(response);
    }

    /**
     * Ends a game.
     * 
     * @param gameId The ID of the game to end
     * @return Success status
     */
    @PostMapping("/games/{gameId}/end")
    public ResponseEntity<Map<String, Object>> endGame(@PathVariable String gameId) {
        boolean ended = gameService.endGame(gameId);
        if (!ended) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ended");
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new AI agent and adds it to the game.
     * 
     * @param gameId The ID of the game to add the agent to
     * @param seatNumber The seat number for the agent
     * @param agentType The type of agent (e.g., "GEMINI_PRO", "CUSTOM")
     * @param modelName The name of the model to use (optional)
     * @param promptTemplate The prompt template to use (optional)
     * @return The created agent details
     */
    @PostMapping("/games/{gameId}/agents")
    public ResponseEntity<Map<String, Object>> createAgent(
            @PathVariable String gameId,
            @RequestParam int seatNumber,
            @RequestParam String agentType,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String promptTemplate) {

        Agent agent = agentService.createAgent(gameId, seatNumber, agentType, modelName, promptTemplate);
        if (agent == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("agentId", agent.getId());
        response.put("seatNumber", agent.getSeatNumber());
        response.put("agentType", agent.getAgentType());
        if (agent.getModelName() != null) {
            response.put("modelName", agent.getModelName());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Gets all registered tools for an agent.
     * 
     * @param gameId The ID of the game
     * @param agentId The ID of the agent
     * @return The list of registered tools
     */
    @GetMapping("/games/{gameId}/agents/{agentId}/tools")
    public ResponseEntity<List<Map<String, Object>>> getAgentTools(
            @PathVariable String gameId,
            @PathVariable String agentId) {

        List<AgentTool> tools = agentService.getRegisteredTools(agentId);
        if (tools.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> response = tools.stream()
                .map(tool -> {
                    Map<String, Object> toolInfo = new HashMap<>();
                    toolInfo.put("name", tool.getName());
                    toolInfo.put("description", tool.getDescription());
                    toolInfo.put("inputSchema", tool.getInputSchema());
                    toolInfo.put("outputSchema", tool.getOutputSchema());
                    toolInfo.put("enabled", tool.isEnabled());
                    return toolInfo;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Executes a tool for an agent.
     * 
     * @param gameId The ID of the game
     * @param agentId The ID of the agent
     * @param toolName The name of the tool to execute
     * @param parameters The parameters for the tool
     * @return The result of the tool execution
     */
    @PostMapping("/games/{gameId}/agents/{agentId}/execute")
    public ResponseEntity<Map<String, Object>> executeAgentTool(
            @PathVariable String gameId,
            @PathVariable String agentId,
            @RequestParam String toolName,
            @RequestBody Map<String, Object> parameters) {

        Map<String, Object> result = agentService.executeTool(agentId, toolName, parameters);
        if (result.containsKey("error")) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Gets the system prompt for an agent.
     * 
     * @param gameId The ID of the game
     * @param agentId The ID of the agent
     * @return The system prompt
     */
    @GetMapping("/games/{gameId}/agents/{agentId}/prompt")
    public ResponseEntity<Map<String, Object>> getAgentPrompt(
            @PathVariable String gameId,
            @PathVariable String agentId) {

        String prompt = agentService.getAgentPrompt(agentId);
        if (prompt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("prompt", prompt);
        return ResponseEntity.ok(response);
    }

    /**
     * Sends a public message to all players in a game.
     *
     * @param gameId The ID of the game
     * @param senderSeat The seat number of the sender
     * @param content The message content
     * @return The created message details
     */
    @PostMapping("/games/{gameId}/chat/public")
    public ResponseEntity<Map<String, Object>> sendPublicMessage(
            @PathVariable String gameId,
            @RequestParam int senderSeat,
            @RequestParam String content) {

        ChatMessage message = chatService.sendPublicMessage(gameId, senderSeat, content);
        if (message == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("messageId", message.getId());
        response.put("senderSeat", message.getSender().getSeatNumber());
        response.put("content", message.getContent());
        response.put("timestamp", message.getTimestamp());
        response.put("isPublic", message.isPublic());
        return ResponseEntity.ok(response);
    }

    /**
     * Sends a private message to a specific player in a game.
     *
     * @param gameId The ID of the game
     * @param senderSeat The seat number of the sender
     * @param recipientSeat The seat number of the recipient
     * @param content The message content
     * @return The created message details
     */
    @PostMapping("/games/{gameId}/chat/private")
    public ResponseEntity<Map<String, Object>> sendPrivateMessage(
            @PathVariable String gameId,
            @RequestParam int senderSeat,
            @RequestParam int recipientSeat,
            @RequestParam String content) {

        ChatMessage message = chatService.sendPrivateMessage(gameId, senderSeat, recipientSeat, content);
        if (message == null) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("messageId", message.getId());
        response.put("senderSeat", message.getSender().getSeatNumber());
        response.put("recipientSeat", message.getRecipient().getSeatNumber());
        response.put("content", message.getContent());
        response.put("timestamp", message.getTimestamp());
        response.put("isPublic", message.isPublic());
        return ResponseEntity.ok(response);
    }

    /**
     * Gets all public messages for a game.
     *
     * @param gameId The ID of the game
     * @return A list of public messages
     */
    @GetMapping("/games/{gameId}/chat/public")
    public ResponseEntity<List<Map<String, Object>>> getPublicMessages(@PathVariable String gameId) {
        List<ChatMessage> messages = chatService.getPublicMessages(gameId);

        List<Map<String, Object>> response = messages.stream()
                .map(m -> {
                    Map<String, Object> messageInfo = new HashMap<>();
                    messageInfo.put("messageId", m.getId());
                    messageInfo.put("senderSeat", m.getSender().getSeatNumber());
                    messageInfo.put("content", m.getContent());
                    messageInfo.put("timestamp", m.getTimestamp());
                    return messageInfo;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets all messages (public and private) visible to a specific player.
     *
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player
     * @return A list of messages visible to the player
     */
    @GetMapping("/games/{gameId}/players/{playerSeat}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessagesForPlayer(
            @PathVariable String gameId,
            @PathVariable int playerSeat) {

        List<ChatMessage> messages = chatService.getMessagesForPlayer(gameId, playerSeat);

        List<Map<String, Object>> response = messages.stream()
                .map(m -> {
                    Map<String, Object> messageInfo = new HashMap<>();
                    messageInfo.put("messageId", m.getId());
                    messageInfo.put("senderSeat", m.getSender().getSeatNumber());
                    if (!m.isPublic() && m.getRecipient() != null) {
                        messageInfo.put("recipientSeat", m.getRecipient().getSeatNumber());
                    }
                    messageInfo.put("content", m.getContent());
                    messageInfo.put("timestamp", m.getTimestamp());
                    messageInfo.put("isPublic", m.isPublic());
                    return messageInfo;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gets private messages between two players.
     *
     * @param gameId The ID of the game
     * @param playerSeat1 The seat number of the first player
     * @param playerSeat2 The seat number of the second player
     * @return A list of private messages between the two players
     */
    @GetMapping("/games/{gameId}/chat/private")
    public ResponseEntity<List<Map<String, Object>>> getPrivateMessagesBetweenPlayers(
            @PathVariable String gameId,
            @RequestParam int playerSeat1,
            @RequestParam int playerSeat2) {

        List<ChatMessage> messages = chatService.getPrivateMessagesBetweenPlayers(gameId, playerSeat1, playerSeat2);

        List<Map<String, Object>> response = messages.stream()
                .map(m -> {
                    Map<String, Object> messageInfo = new HashMap<>();
                    messageInfo.put("messageId", m.getId());
                    messageInfo.put("senderSeat", m.getSender().getSeatNumber());
                    messageInfo.put("recipientSeat", m.getRecipient().getSeatNumber());
                    messageInfo.put("content", m.getContent());
                    messageInfo.put("timestamp", m.getTimestamp());
                    return messageInfo;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
