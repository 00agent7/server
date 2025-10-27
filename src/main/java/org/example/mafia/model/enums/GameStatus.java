package org.example.mafia.model.enums;

/**
 * Represents the different statuses a game can have.
 * According to the rules, a game can end with a Red win, Black win, or a tie.
 */
public enum GameStatus {
    // Game is in progress
    IN_PROGRESS,
    
    // Game has ended with Red team (Citizens) winning
    RED_WIN,
    
    // Game has ended with Black team (Mafia) winning
    BLACK_WIN,
    
    // Game has ended in a tie
    TIE;
    
    /**
     * Determines if the game has ended.
     * 
     * @return true if the game status is not IN_PROGRESS, false otherwise
     */
    public boolean isGameOver() {
        return this != IN_PROGRESS;
    }
}