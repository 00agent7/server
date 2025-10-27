package org.example.mafia.model.enums;

/**
 * Represents the different phases of the Mafia game.
 * According to the rules, the game alternates between Night, Day, and Voting phases.
 */
public enum GamePhase {
    // Initial setup phase
    SETUP,
    
    // Night phases
    NIGHT_0,        // Initial night for role introductions
    NIGHT_MAFIA,    // Mafia members wake up and choose a target
    NIGHT_DON,      // Don checks a player for Sheriff role
    NIGHT_SHERIFF,  // Sheriff checks a player for Mafia role
    NIGHT_RESULT,   // Results of the night actions are processed
    
    // Day phase - players discuss and make nominations
    DAY,
    
    // Voting phase - players vote on nominations
    VOTING,
    
    // Game end phase
    GAME_OVER;
    
    /**
     * Determines if this is a night phase.
     * 
     * @return true if this is any night phase, false otherwise
     */
    public boolean isNightPhase() {
        return this == NIGHT_0 || this == NIGHT_MAFIA || 
               this == NIGHT_DON || this == NIGHT_SHERIFF || 
               this == NIGHT_RESULT;
    }
    
    /**
     * Gets the next phase in the game sequence.
     * 
     * @return the next game phase
     */
    public GamePhase getNextPhase() {
        switch (this) {
            case SETUP:
                return NIGHT_0;
            case NIGHT_0:
                return DAY;
            case DAY:
                return VOTING;
            case VOTING:
                return NIGHT_MAFIA;
            case NIGHT_MAFIA:
                return NIGHT_DON;
            case NIGHT_DON:
                return NIGHT_SHERIFF;
            case NIGHT_SHERIFF:
                return NIGHT_RESULT;
            case NIGHT_RESULT:
                return DAY;
            case GAME_OVER:
            default:
                return GAME_OVER;
        }
    }
}