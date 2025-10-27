package org.example.mafia.service;

import org.example.mafia.model.ChatMessage;
import org.example.mafia.model.Game;
import org.example.mafia.model.Player;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing chat messages between players in Mafia games.
 */
@Service
public class ChatService {
    private final GameService gameService;
    private final Map<String, List<ChatMessage>> gameMessages = new ConcurrentHashMap<>();

    @Autowired
    public ChatService(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * Sends a public message to all players in a game.
     *
     * @param gameId The ID of the game
     * @param senderSeat The seat number of the sender
     * @param content The message content
     * @return The created message, or null if the message could not be sent
     */
    public ChatMessage sendPublicMessage(String gameId, int senderSeat, String content) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return null;
        }

        Player sender = game.getPlayerBySeat(senderSeat);
        if (sender == null || !sender.isAlive()) {
            return null; // Only alive players can send messages
        }

        ChatMessage message = new ChatMessage(game, sender, content);
        addMessageToGame(gameId, message);
        return message;
    }

    /**
     * Sends a private message to a specific player in a game.
     *
     * @param gameId The ID of the game
     * @param senderSeat The seat number of the sender
     * @param recipientSeat The seat number of the recipient
     * @param content The message content
     * @return The created message, or null if the message could not be sent
     */
    public ChatMessage sendPrivateMessage(String gameId, int senderSeat, int recipientSeat, String content) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return null;
        }

        Player sender = game.getPlayerBySeat(senderSeat);
        Player recipient = game.getPlayerBySeat(recipientSeat);
        
        if (sender == null || recipient == null || !sender.isAlive()) {
            return null; // Sender must be alive, recipient must exist
        }

        ChatMessage message = new ChatMessage(game, sender, recipient, content, false);
        addMessageToGame(gameId, message);
        return message;
    }

    /**
     * Gets all public messages for a game.
     *
     * @param gameId The ID of the game
     * @return A list of public messages, or an empty list if no messages are found
     */
    public List<ChatMessage> getPublicMessages(String gameId) {
        List<ChatMessage> messages = gameMessages.get(gameId);
        if (messages == null) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(ChatMessage::isPublic)
                .collect(Collectors.toList());
    }

    /**
     * Gets all messages (public and private) visible to a specific player.
     *
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player
     * @return A list of messages visible to the player, or an empty list if no messages are found
     */
    public List<ChatMessage> getMessagesForPlayer(String gameId, int playerSeat) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return new ArrayList<>();
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> messages = gameMessages.get(gameId);
        if (messages == null) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(m -> m.isPublic() || 
                        m.getSender().equals(player) || 
                        (m.getRecipient() != null && m.getRecipient().equals(player)))
                .collect(Collectors.toList());
    }

    /**
     * Gets private messages between two players.
     *
     * @param gameId The ID of the game
     * @param playerSeat1 The seat number of the first player
     * @param playerSeat2 The seat number of the second player
     * @return A list of private messages between the two players, or an empty list if no messages are found
     */
    public List<ChatMessage> getPrivateMessagesBetweenPlayers(String gameId, int playerSeat1, int playerSeat2) {
        Game game = gameService.getGame(gameId);
        if (game == null) {
            return new ArrayList<>();
        }

        Player player1 = game.getPlayerBySeat(playerSeat1);
        Player player2 = game.getPlayerBySeat(playerSeat2);
        if (player1 == null || player2 == null) {
            return new ArrayList<>();
        }

        List<ChatMessage> messages = gameMessages.get(gameId);
        if (messages == null) {
            return new ArrayList<>();
        }

        return messages.stream()
                .filter(m -> !m.isPublic())
                .filter(m -> (m.getSender().equals(player1) && m.getRecipient().equals(player2)) ||
                        (m.getSender().equals(player2) && m.getRecipient().equals(player1)))
                .collect(Collectors.toList());
    }

    /**
     * Adds a message to the game's message list.
     *
     * @param gameId The ID of the game
     * @param message The message to add
     */
    private void addMessageToGame(String gameId, ChatMessage message) {
        gameMessages.computeIfAbsent(gameId, k -> new ArrayList<>()).add(message);
    }
}