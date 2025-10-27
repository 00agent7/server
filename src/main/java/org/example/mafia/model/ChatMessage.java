package org.example.mafia.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a chat message in the Mafia game.
 * Messages can be sent to all players (public) or to specific players (private).
 */
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    private String id;

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private Player sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private Player recipient;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean isPublic;

    /**
     * Creates a new chat message.
     * 
     * @param game The game the message belongs to
     * @param sender The player sending the message
     * @param recipient The player receiving the message (null for public messages)
     * @param content The message content
     * @param isPublic Whether the message is public (visible to all players)
     */
    public ChatMessage(Game game, Player sender, Player recipient, String content, boolean isPublic) {
        this.id = UUID.randomUUID().toString();
        this.game = game;
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.isPublic = isPublic;
    }

    /**
     * Creates a new public chat message.
     * 
     * @param game The game the message belongs to
     * @param sender The player sending the message
     * @param content The message content
     */
    public ChatMessage(Game game, Player sender, String content) {
        this(game, sender, null, content, true);
    }

    /**
     * No-arg constructor for JPA.
     */
    protected ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    // Getters

    public String getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public Player getSender() {
        return sender;
    }

    public Player getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isPublic() {
        return isPublic;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", sender=" + sender.getSeatNumber() +
                ", recipient=" + (recipient != null ? recipient.getSeatNumber() : "all") +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", isPublic=" + isPublic +
                '}';
    }
}