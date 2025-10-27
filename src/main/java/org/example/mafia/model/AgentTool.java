package org.example.mafia.model;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Represents a tool that can be used by an AI agent in the Mafia game.
 * Each tool corresponds to a game function that the agent can invoke.
 */
@Entity
@Table(name = "agent_tools")
public class AgentTool {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String functionName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String inputSchema;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String outputSchema;

    @Column(nullable = false)
    private boolean isEnabled;

    /**
     * Creates a new agent tool with the given parameters.
     * 
     * @param name The name of the tool
     * @param description The description of what the tool does
     * @param functionName The name of the function to invoke
     * @param inputSchema The JSON schema for the tool's input parameters
     * @param outputSchema The JSON schema for the tool's output
     */
    public AgentTool(String name, String description, String functionName, 
                    String inputSchema, String outputSchema) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.functionName = functionName;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.isEnabled = true;
    }

    /**
     * No-arg constructor for JPA.
     */
    protected AgentTool() {
        this.id = UUID.randomUUID().toString();
        this.isEnabled = true;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public String toString() {
        return "AgentTool{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", functionName='" + functionName + '\'' +
                ", isEnabled=" + isEnabled +
                '}';
    }
}