package org.example.mafia.service;

import org.example.mafia.model.Agent;
import org.example.mafia.model.AgentTool;
import org.example.mafia.model.Game;
import org.example.mafia.model.Player;
import org.example.mafia.model.enums.PlayerRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Service for managing AI agents in the Mafia game.
 * This service provides methods for creating agents, registering tools, and executing agent actions.
 */
@Service
public class AgentService {
    private final GameService gameService;
    private final Map<String, List<AgentTool>> registeredTools = new ConcurrentHashMap<>();
    private final Map<String, String> agentPrompts = new ConcurrentHashMap<>();

    @Autowired
    public AgentService(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Creates a new AI agent and adds it to the game.
     *
     * @param gameId The ID of the game to add the agent to
     * @param seatNumber The seat number for the agent
     * @param agentType The type of agent (e.g., "GEMINI_PRO", "CUSTOM")
     * @param modelName The name of the model to use (optional)
     * @param promptTemplate The prompt template to use (optional)
     * @return The created agent, or null if the agent could not be created
     */
    public Agent createAgent(String gameId, int seatNumber, String agentType, String modelName, String promptTemplate) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return null;
        }

        if (seatNumber < 1 || seatNumber > 10) {
            return null; // Seat number must be between 1 and 10
        }

        Agent agent = new Agent(seatNumber, agentType);
        if (modelName != null) {
            agent.setModelName(modelName);
        }
        if (promptTemplate != null) {
            agent.setPromptTemplate(promptTemplate);
        }

        boolean added = game.addPlayer(agent);
        if (!added) {
            return null;
        }

        // Register the game functions as tools
        registerGameTools(agent.getId(), gameId);
        
        // Store the agent's system prompt
        agentPrompts.put(agent.getId(), buildSystemPrompt(agent));

        return agent;
    }

    /**
     * Builds a system prompt for the given agent based on its role.
     *
     * @param agent The agent to build the prompt for
     * @return The system prompt
     */
    private String buildSystemPrompt(Agent agent) {
        if (agent.getPromptTemplate() != null) {
            return agent.getPromptTemplate();
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI agent playing the Mafia game. ");
        prompt.append("Your seat number is ").append(agent.getSeatNumber()).append(". ");

        PlayerRole role = agent.getRole();
        if (role != null) {
            prompt.append("Your role is ").append(role.name()).append(". ");
            
            if (role == PlayerRole.MAFIA || role == PlayerRole.DON) {
                prompt.append("You are on the Mafia team. Your goal is to eliminate all civilians. ");
            } else {
                prompt.append("You are on the Civilian team. Your goal is to identify and eliminate all Mafia members. ");
            }
            
            if (role == PlayerRole.SHERIFF) {
                prompt.append("As the Sheriff, you can check one player each night to determine if they are Mafia. ");
            } else if (role == PlayerRole.DON) {
                prompt.append("As the Don, you can check one player each night to determine if they are the Sheriff. ");
            }
        }

        prompt.append("Use the available tools to interact with the game and make strategic decisions. ");
        prompt.append("Be convincing in your role and try to win the game for your team.");

        return prompt.toString();
    }

    /**
     * Executes a tool for an agent.
     *
     * @param agentId The ID of the agent
     * @param toolName The name of the tool to execute
     * @param parameters The parameters for the tool
     * @return The result of the tool execution
     */
    public Map<String, Object> executeTool(String agentId, String toolName, Map<String, Object> parameters) {
        // Find the tool with the given name
        AgentTool tool = registeredTools.getOrDefault(agentId, new ArrayList<>()).stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst()
                .orElse(null);

        if (tool == null || !tool.isEnabled()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Tool not found or disabled: " + toolName);
            return error;
        }

        // Get the game ID from the tool's function name (assuming it's stored in the tool)
        String gameId = tool.getFunctionName().split(":")[0]; // Format: "gameId:functionName"
        String functionName = tool.getFunctionName().split(":")[1];

        // Execute the tool based on its function name
        try {
            return executeGameFunction(functionName, gameId, agentId, parameters);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error executing tool: " + e.getMessage());
            return error;
        }
    }

    /**
     * Executes a game function based on the function name and parameters.
     *
     * @param functionName The name of the function to execute
     * @param gameId The ID of the game
     * @param agentId The ID of the agent
     * @param parameters The parameters for the function
     * @return The result of the function execution
     */
    private Map<String, Object> executeGameFunction(String functionName, String gameId, String agentId, Map<String, Object> parameters) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Game not found");
            return error;
        }

        Player agent = game.getPlayers().stream()
                .filter(p -> p.getId().equals(agentId))
                .findFirst()
                .orElse(null);

        if (agent == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Agent not found in game");
            return error;
        }

        int agentSeat = agent.getSeatNumber();

        // Execute the appropriate function based on the function name
        switch (functionName) {
            case "getGameState":
                return getGameState(game);
            case "createNomination":
                return createNomination(game, agentSeat, parameters);
            case "vote":
                return vote(game, agentSeat, parameters);
            case "checkSheriff":
                return checkSheriff(game, agentSeat, parameters);
            case "checkMafia":
                return checkMafia(game, agentSeat, parameters);
            case "mafiaKill":
                return mafiaKill(game, agentSeat, parameters);
            case "recordPrimaNotaGuesses":
                return recordPrimaNotaGuesses(game, agentSeat, parameters);
            default:
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Unknown function: " + functionName);
                return error;
        }
    }

    /**
     * Gets the current game state.
     *
     * @param game The game
     * @return The game state
     */
    private Map<String, Object> getGameState(Game game) {
        Map<String, Object> state = new HashMap<>();
        state.put("gameId", game.getId());
        state.put("phase", game.getCurrentPhase());
        state.put("status", game.getStatus());
        state.put("dayCount", game.getDayCount());
        state.put("nightCount", game.getNightCount());
        
        // Add player information
        List<Map<String, Object>> players = new ArrayList<>();
        for (Player player : game.getPlayers()) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("seatNumber", player.getSeatNumber());
            playerInfo.put("alive", player.isAlive());
            players.add(playerInfo);
        }
        state.put("players", players);
        
        return state;
    }

    /**
     * Creates a nomination.
     *
     * @param game The game
     * @param agentSeat The seat number of the agent
     * @param parameters The parameters for the nomination
     * @return The result of the nomination
     */
    private Map<String, Object> createNomination(Game game, int agentSeat, Map<String, Object> parameters) {
        Integer nomineeSeat = (Integer) parameters.get("nomineeSeat");
        if (nomineeSeat == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Missing nomineeSeat parameter");
            return error;
        }

        return executeWithGameService(
            () -> gameService.createNomination(game.getId(), agentSeat, nomineeSeat),
            nomination -> {
                if (nomination == null) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("error", "Failed to create nomination");
                    return error;
                }
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("nominatorSeat", nomination.getNominator().getSeatNumber());
                result.put("nomineeSeat", nomination.getNominee().getSeatNumber());
                return result;
            }
        );
    }

    /**
     * Votes for a nomination.
     *
     * @param game The game
     * @param agentSeat The seat number of the agent
     * @param parameters The parameters for the vote
     * @return The result of the vote
     */
    private Map<String, Object> vote(Game game, int agentSeat, Map<String, Object> parameters) {
        Integer nominationIndex = (Integer) parameters.get("nominationIndex");
        if (nominationIndex == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Missing nominationIndex parameter");
            return error;
        }

        boolean voted = gameService.vote(game.getId(), nominationIndex, agentSeat);
        if (!voted) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to vote");
            return error;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * Checks if a player is the Sheriff.
     *
     * @param game The game
     * @param agentSeat The seat number of the agent
     * @param parameters The parameters for the check
     * @return The result of the check
     */
    private Map<String, Object> checkSheriff(Game game, int agentSeat, Map<String, Object> parameters) {
        Integer targetSeat = (Integer) parameters.get("targetSeat");
        if (targetSeat == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Missing targetSeat parameter");
            return error;
        }

        Boolean isSheriff = gameService.checkSheriff(game.getId(), agentSeat, targetSeat);
        if (isSheriff == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to check Sheriff");
            return error;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("isSheriff", isSheriff);
        return result;
    }

    /**
     * Checks if a player is Mafia.
     *
     * @param game The game
     * @param agentSeat The seat number of the agent
     * @param parameters The parameters for the check
     * @return The result of the check
     */
    private Map<String, Object> checkMafia(Game game, int agentSeat, Map<String, Object> parameters) {
        Integer targetSeat = (Integer) parameters.get("targetSeat");
        if (targetSeat == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Missing targetSeat parameter");
            return error;
        }

        Boolean isMafia = gameService.checkMafia(game.getId(), agentSeat, targetSeat);
        if (isMafia == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to check Mafia");
            return error;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("isMafia", isMafia);
        return result;
    }

    /**
     * Performs a Mafia kill.
     *
     * @param game The game
     * @param agentSeat The seat number of the agent
     * @param parameters The parameters for the kill
     * @return The result of the kill
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> mafiaKill(Game game, int agentSeat, Map<String, Object> parameters) {
        List<Integer> mafiaSeats = (List<Integer>) parameters.get("mafiaSeats");
        Integer targetSeat = (Integer) parameters.get("targetSeat");
        
        if (mafiaSeats == null || targetSeat == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Missing mafiaSeats or targetSeat parameter");
            return error;
        }

        boolean killed = gameService.mafiaKill(game.getId(), mafiaSeats, targetSeat);
        if (!killed) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to perform Mafia kill");
            return error;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * Records Prima Nota guesses.
     *
     * @param game The game
     * @param agentSeat The seat number of the agent
     * @param parameters The parameters for the guesses
     * @return The result of recording the guesses
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> recordPrimaNotaGuesses(Game game, int agentSeat, Map<String, Object> parameters) {
        List<Integer> guessesList = (List<Integer>) parameters.get("guesses");
        if (guessesList == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Missing guesses parameter");
            return error;
        }

        int[] guesses = guessesList.stream().mapToInt(Integer::intValue).toArray();
        boolean recorded = gameService.recordPrimaNotaGuesses(game.getId(), agentSeat, guesses);
        if (!recorded) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to record Prima Nota guesses");
            return error;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        return result;
    }

    /**
     * Helper method to execute a function with the game service and map the result.
     *
     * @param supplier The function to execute
     * @param resultMapper The function to map the result
     * @return The mapped result
     */
    private <T> Map<String, Object> executeWithGameService(java.util.function.Supplier<T> supplier, java.util.function.Function<T, Map<String, Object>> resultMapper) {
        try {
            T result = supplier.get();
            return resultMapper.apply(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Error: " + e.getMessage());
            return error;
        }
    }

    /**
     * Registers the game functions as tools for the agent.
     *
     * @param agentId The ID of the agent
     * @param gameId The ID of the game
     */
    private void registerGameTools(String agentId, String gameId) {
        List<AgentTool> tools = new ArrayList<>();
        registeredTools.put(agentId, tools);
        
        // Register getGameState tool
        tools.add(new AgentTool(
            "getGameState",
            "Get the current state of the game",
            gameId + ":getGameState",
            "{}",
            "{\"gameId\": \"string\", \"phase\": \"string\", \"status\": \"string\", \"dayCount\": \"number\", \"nightCount\": \"number\", \"players\": [{\"seatNumber\": \"number\", \"alive\": \"boolean\"}]}"
        ));
        
        // Register createNomination tool
        tools.add(new AgentTool(
            "createNomination",
            "Nominate a player for elimination",
            gameId + ":createNomination",
            "{\"nomineeSeat\": \"number\"}",
            "{\"success\": \"boolean\", \"nominatorSeat\": \"number\", \"nomineeSeat\": \"number\"}"
        ));
        
        // Register vote tool
        tools.add(new AgentTool(
            "vote",
            "Vote for a nomination",
            gameId + ":vote",
            "{\"nominationIndex\": \"number\"}",
            "{\"success\": \"boolean\"}"
        ));
        
        // Register checkSheriff tool
        tools.add(new AgentTool(
            "checkSheriff",
            "Check if a player is the Sheriff (Don only)",
            gameId + ":checkSheriff",
            "{\"targetSeat\": \"number\"}",
            "{\"isSheriff\": \"boolean\"}"
        ));
        
        // Register checkMafia tool
        tools.add(new AgentTool(
            "checkMafia",
            "Check if a player is Mafia (Sheriff only)",
            gameId + ":checkMafia",
            "{\"targetSeat\": \"number\"}",
            "{\"isMafia\": \"boolean\"}"
        ));
        
        // Register mafiaKill tool
        tools.add(new AgentTool(
            "mafiaKill",
            "Perform a Mafia kill (Mafia only)",
            gameId + ":mafiaKill",
            "{\"mafiaSeats\": [\"number\"], \"targetSeat\": \"number\"}",
            "{\"success\": \"boolean\"}"
        ));
        
        // Register recordPrimaNotaGuesses tool
        tools.add(new AgentTool(
            "recordPrimaNotaGuesses",
            "Record Prima Nota guesses",
            gameId + ":recordPrimaNotaGuesses",
            "{\"guesses\": [\"number\"]}",
            "{\"success\": \"boolean\"}"
        ));
    }

    /**
     * Gets all registered tools for an agent.
     *
     * @param agentId The ID of the agent
     * @return The list of registered tools
     */
    public List<AgentTool> getRegisteredTools(String agentId) {
        return registeredTools.getOrDefault(agentId, new ArrayList<>());
    }
    
    /**
     * Gets the system prompt for an agent.
     *
     * @param agentId The ID of the agent
     * @return The system prompt
     */
    public String getAgentPrompt(String agentId) {
        return agentPrompts.getOrDefault(agentId, "");
    }
}