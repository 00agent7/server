package org.example.mafia.model;

/**
 * Represents the warnings and sanctions applied to a player in a Mafia game.
 * According to the rules, players can receive warnings for various infractions,
 * and accumulating warnings leads to penalties.
 */
public class PlayerSanctions {
    private int warningCount;
    private boolean removed;
    private boolean yellowCard;
    private boolean redCard;
    
    /**
     * Creates a new PlayerSanctions instance with no warnings or sanctions.
     */
    public PlayerSanctions() {
        this.warningCount = 0;
        this.removed = false;
        this.yellowCard = false;
        this.redCard = false;
    }
    
    /**
     * Adds a warning to the player.
     * According to the rules, 3 warnings result in losing the next speech,
     * and 4 warnings result in removal from the game.
     * 
     * @return true if the player should be removed from the game, false otherwise
     */
    public boolean addWarning() {
        warningCount++;
        
        if (warningCount >= 4) {
            removed = true;
            return true;
        }
        
        return false;
    }
    
    /**
     * Marks the player as removed from the game.
     * This is for serious violations like shouting after Night is called,
     * peeking, leaving the table without permission, etc.
     */
    public void removePlayer() {
        removed = true;
    }
    
    /**
     * Issues a yellow card to the player.
     * This is for gross misconduct like deliberate peeking/cheating,
     * extreme disrespect, etc.
     */
    public void issueYellowCard() {
        yellowCard = true;
    }
    
    /**
     * Issues a red card to the player.
     * This is for critical misconduct and results in disqualification.
     */
    public void issueRedCard() {
        redCard = true;
    }
    
    /**
     * Checks if the player has lost their next speech.
     * According to the rules, this happens when a player has 3 warnings.
     * 
     * @return true if the player has lost their next speech, false otherwise
     */
    public boolean hasLostNextSpeech() {
        return warningCount >= 3;
    }
    
    /**
     * Gets the number of warnings the player has received.
     * 
     * @return The warning count
     */
    public int getWarningCount() {
        return warningCount;
    }
    
    /**
     * Checks if the player has been removed from the game.
     * 
     * @return true if the player has been removed, false otherwise
     */
    public boolean isRemoved() {
        return removed;
    }
    
    /**
     * Checks if the player has received a yellow card.
     * 
     * @return true if the player has a yellow card, false otherwise
     */
    public boolean hasYellowCard() {
        return yellowCard;
    }
    
    /**
     * Checks if the player has received a red card.
     * 
     * @return true if the player has a red card, false otherwise
     */
    public boolean hasRedCard() {
        return redCard;
    }
}