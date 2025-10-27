package org.example.mafia.model.enums;

/**
 * Represents the different roles a player can have in the Mafia game.
 * According to the rules, there are 7 Citizens (one is Sheriff) and 3 Mafia (one is Don).
 */
public enum PlayerRole {
    // Red team (Citizens)
    CITIZEN,
    SHERIFF,
    
    // Black team (Mafia)
    MAFIA,
    DON;
    
    /**
     * Determines if the role is on the Red team (Citizens).
     * 
     * @return true if the role is CITIZEN or SHERIFF, false otherwise
     */
    public boolean isRed() {
        return this == CITIZEN || this == SHERIFF;
    }
    
    /**
     * Determines if the role is on the Black team (Mafia).
     * 
     * @return true if the role is MAFIA or DON, false otherwise
     */
    public boolean isBlack() {
        return this == MAFIA || this == DON;
    }
}