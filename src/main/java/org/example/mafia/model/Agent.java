package org.example.mafia.model;

import jakarta.persistence.*;
import org.example.mafia.model.enums.PlayerRole;

import java.util.UUID;

/**
 * Represents an AI agent in the Mafia game.
 * This class extends the Player class to add agent-specific functionality.
 */
@Entity
@DiscriminatorValue("AGENT")
public class Agent extends Player {

    @Column(nullable = false)
    private String agentType;

    @Column(nullable = true)
    private String modelName;

    @Column(nullable = true)
    private String promptTemplate;

    @Column(nullable = false)
    private boolean isActive;

    /**
     * Creates a new agent with the given ID, seat number, and agent type.
     * 
     * @param id The unique identifier for the agent
     * @param seatNumber The seat number assigned to the agent (1-10)
     * @param agentType The type of agent (e.g., "GEMINI_PRO", "CUSTOM")
     */
    public Agent(String id, int seatNumber, String agentType) {
        super(id, seatNumber);
        this.agentType = agentType;
        this.isActive = true;
    }

    /**
     * Creates a new agent with a random ID, the given seat number, and agent type.
     * 
     * @param seatNumber The seat number assigned to the agent (1-10)
     * @param agentType The type of agent (e.g., "GEMINI_PRO", "CUSTOM")
     */
    public Agent(int seatNumber, String agentType) {
        this(UUID.randomUUID().toString(), seatNumber, agentType);
    }

    /**
     * No-arg constructor for JPA.
     */
    protected Agent() {
        super();
        this.agentType = "GEMINI_PRO";
        this.isActive = true;
    }

    // Getters and setters

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Agent{" +
                "id='" + getId() + '\'' +
                ", seatNumber=" + getSeatNumber() +
                ", role=" + getRole() +
                ", agentType='" + agentType + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
