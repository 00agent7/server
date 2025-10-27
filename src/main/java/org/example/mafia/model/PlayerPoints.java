package org.example.mafia.model;

/**
 * Represents the points earned by a player in a Mafia game.
 * According to the rules, players earn points based on their role, game outcome,
 * and special actions or performances.
 */
public class PlayerPoints {
    // Base points
    private static final int WIN_POINTS = 4;
    private static final int LOSE_POINTS = 1;
    private static final int TIE_POINTS = 0;

    // Bonus points
    private static final double PRIMA_NOTA_PARTIAL_BONUS = 0.5;
    private static final double PRIMA_NOTA_FULL_BONUS = 1.0;
    private static final double PRIMA_NOTA_SHERIFF_BONUS = 0.5;
    private static final double BEST_MOVE_SMALL_BONUS = 0.5;
    private static final double BEST_MOVE_LARGE_BONUS = 1.0;
    private static final double BEST_PLAY_SMALL_BONUS = 1.5;
    private static final double BEST_PLAY_LARGE_BONUS = 2.0;

    // Penalty points
    private static final double WORST_MOVE_SMALL_PENALTY = -0.5;
    private static final double WORST_MOVE_LARGE_PENALTY = -1.0;
    private static final double WORST_PLAY_PENALTY = -1.5;
    private static final double REMOVAL_PENALTY = -1.5;
    private static final double YELLOW_CARD_PENALTY = -2.0;
    private static final double RED_CARD_PENALTY = -2.0;

    // Compensation points
    private static final double COMPENSATION_SECOND_LOSS = 1.0;
    private static final double COMPENSATION_THIRD_LOSS = 2.0;
    private static final double COMPENSATION_FOURTH_PLUS_LOSS = 3.0;

    // Maximum bonus points per game
    private static final double MAX_BONUS_POINTS = 5.0;

    private double basePoints;
    private double bonusPoints;
    private double penaltyPoints;
    private double compensationPoints;

    /**
     * Creates a new PlayerPoints instance with zero points.
     */
    public PlayerPoints() {
        this.basePoints = 0;
        this.bonusPoints = 0;
        this.penaltyPoints = 0;
        this.compensationPoints = 0;
    }

    /**
     * Calculates base points based on game outcome.
     * 
     * @param isWinner Whether the player's team won
     * @param isTie Whether the game ended in a tie
     * @return The base points earned
     */
    public double calculateBasePoints(boolean isWinner, boolean isTie) {
        if (isTie) {
            return TIE_POINTS;
        } else if (isWinner) {
            return WIN_POINTS;
        } else {
            return LOSE_POINTS;
        }
    }

    /**
     * Adds Prima Nota bonus points based on the number of correct guesses.
     * 
     * @param correctGuesses The number of correct Prima Nota guesses
     * @param isSheriff Whether the player is the Sheriff
     */
    public void addPrimaNotaBonus(int correctGuesses, boolean isSheriff) {
        if (isSheriff && correctGuesses >= 2) {
            bonusPoints += PRIMA_NOTA_SHERIFF_BONUS;
        } else if (!isSheriff) {
            if (correctGuesses == 2) {
                bonusPoints += PRIMA_NOTA_PARTIAL_BONUS;
            } else if (correctGuesses == 3) {
                bonusPoints += PRIMA_NOTA_FULL_BONUS;
            }
        }

        // Cap bonus points
        capBonusPoints();
    }

    /**
     * Adds Best Move bonus points.
     * 
     * @param isLargeBonus Whether to add the large bonus
     */
    public void addBestMoveBonus(boolean isLargeBonus) {
        bonusPoints += isLargeBonus ? BEST_MOVE_LARGE_BONUS : BEST_MOVE_SMALL_BONUS;

        // Cap bonus points
        capBonusPoints();
    }

    /**
     * Adds Best Play bonus points.
     * 
     * @param isLargeBonus Whether to add the large bonus
     */
    public void addBestPlayBonus(boolean isLargeBonus) {
        bonusPoints += isLargeBonus ? BEST_PLAY_LARGE_BONUS : BEST_PLAY_SMALL_BONUS;

        // Cap bonus points
        capBonusPoints();
    }

    /**
     * Adds Worst Move penalty points.
     * 
     * @param isLargePenalty Whether to add the large penalty
     */
    public void addWorstMovePenalty(boolean isLargePenalty) {
        penaltyPoints += isLargePenalty ? WORST_MOVE_LARGE_PENALTY : WORST_MOVE_SMALL_PENALTY;
    }

    /**
     * Adds Worst Play penalty points.
     */
    public void addWorstPlayPenalty() {
        penaltyPoints += WORST_PLAY_PENALTY;
    }

    /**
     * Adds Removal penalty points.
     */
    public void addRemovalPenalty() {
        penaltyPoints += REMOVAL_PENALTY;
    }

    /**
     * Adds Yellow Card penalty points.
     */
    public void addYellowCardPenalty() {
        penaltyPoints += YELLOW_CARD_PENALTY;
    }

    /**
     * Adds Red Card penalty points.
     */
    public void addRedCardPenalty() {
        penaltyPoints += RED_CARD_PENALTY;
    }

    /**
     * Adds compensation points for First-Killed Players who are Reds and lose multiple times.
     * 
     * @param lossCount The number of times the player has been killed on Night 1 and lost
     */
    public void addCompensationPoints(int lossCount) {
        if (lossCount == 2) {
            compensationPoints += COMPENSATION_SECOND_LOSS;
        } else if (lossCount == 3) {
            compensationPoints += COMPENSATION_THIRD_LOSS;
        } else if (lossCount >= 4) {
            compensationPoints += COMPENSATION_FOURTH_PLUS_LOSS;
        }
    }

    /**
     * Caps the bonus points at the maximum allowed value.
     */
    private void capBonusPoints() {
        if (bonusPoints > MAX_BONUS_POINTS) {
            bonusPoints = MAX_BONUS_POINTS;
        }
    }

    /**
     * Gets the total points earned.
     * 
     * @return The total points
     */
    public double getTotalPoints() {
        return basePoints + bonusPoints + penaltyPoints + compensationPoints;
    }

    /**
     * Sets the base points.
     * 
     * @param basePoints The base points to set
     */
    public void setBasePoints(double basePoints) {
        this.basePoints = basePoints;
    }

    /**
     * Gets the base points.
     * 
     * @return The base points
     */
    public double getBasePoints() {
        return basePoints;
    }

    /**
     * Gets the bonus points.
     * 
     * @return The bonus points
     */
    public double getBonusPoints() {
        return bonusPoints;
    }

    /**
     * Gets the penalty points.
     * 
     * @return The penalty points
     */
    public double getPenaltyPoints() {
        return penaltyPoints;
    }

    /**
     * Gets the compensation points.
     * 
     * @return The compensation points
     */
    public double getCompensationPoints() {
        return compensationPoints;
    }
}
