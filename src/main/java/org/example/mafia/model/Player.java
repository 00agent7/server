package org.example.mafia.model;

import jakarta.persistence.*;
import org.example.mafia.model.enums.GameStatus;
import org.example.mafia.model.enums.PlayerRole;

import java.util.*;
import java.util.UUID;

/**
 * Represents a player in the Mafia game.
 * Each player has a unique ID, a seat number, and a role.
 */
@Entity
@Table(name = "players")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "player_type")
@DiscriminatorValue("PLAYER")
public class Player {
    @Id
    private String id;

    @Column(nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    private PlayerRole role;

    private boolean alive;
    private boolean masked;

    // For tracking special actions
    private boolean hasSpoken;
    private boolean hasVoted;
    private boolean hasChecked;
    private boolean hasNominated;

    // For Prima Nota (First Killed Player)
    private boolean isPrimaNotaPlayer;

    @ElementCollection
    @CollectionTable(name = "player_prima_nota_guesses", joinColumns = @JoinColumn(name = "player_id"))
    @Column(name = "seat_number")
    private List<Integer> primaNotaGuesses;

    // For tracking points
    @Embedded
    private PlayerPoints points;

    // For tracking warnings and sanctions
    @Embedded
    private PlayerSanctions sanctions;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    /**
     * Creates a new player with the given ID and seat number.
     * 
     * @param id The unique identifier for the player (usually an AI agent ID)
     * @param seatNumber The seat number assigned to the player (1-10)
     */
    public Player(String id, int seatNumber) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.alive = true;
        this.masked = false;
        this.hasSpoken = false;
        this.hasVoted = false;
        this.hasChecked = false;
        this.hasNominated = false;
        this.isPrimaNotaPlayer = false;
        this.points = new PlayerPoints();
        this.sanctions = new PlayerSanctions();
    }

    /**
     * Creates a new player with a random ID and the given seat number.
     * 
     * @param seatNumber The seat number assigned to the player (1-10)
     */
    public Player(int seatNumber) {
        this(UUID.randomUUID().toString(), seatNumber);
    }

    /**
     * No-arg constructor for JPA.
     */
    protected Player() {
        this.id = UUID.randomUUID().toString();
        this.alive = true;
        this.masked = false;
        this.hasSpoken = false;
        this.hasVoted = false;
        this.hasChecked = false;
        this.hasNominated = false;
        this.isPrimaNotaPlayer = false;
        this.points = new PlayerPoints();
        this.sanctions = new PlayerSanctions();
        this.primaNotaGuesses = new ArrayList<>();
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isMasked() {
        return masked;
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
    }

    public boolean hasSpoken() {
        return hasSpoken;
    }

    public void setHasSpoken(boolean hasSpoken) {
        this.hasSpoken = hasSpoken;
    }

    public boolean hasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public boolean hasChecked() {
        return hasChecked;
    }

    public void setHasChecked(boolean hasChecked) {
        this.hasChecked = hasChecked;
    }

    public boolean hasNominated() {
        return hasNominated;
    }

    public void setHasNominated(boolean hasNominated) {
        this.hasNominated = hasNominated;
    }

    public boolean isPrimaNotaPlayer() {
        return isPrimaNotaPlayer;
    }

    public void setPrimaNotaPlayer(boolean primaNotaPlayer) {
        this.isPrimaNotaPlayer = primaNotaPlayer;
    }

    public List<Integer> getPrimaNotaGuesses() {
        return primaNotaGuesses;
    }

    public void setPrimaNotaGuesses(int[] guesses) {
        if (guesses == null) {
            this.primaNotaGuesses = null;
            return;
        }

        this.primaNotaGuesses = new ArrayList<>();
        for (int guess : guesses) {
            this.primaNotaGuesses.add(guess);
        }
    }

    public void setPrimaNotaGuessesList(List<Integer> primaNotaGuesses) {
        this.primaNotaGuesses = primaNotaGuesses;
    }

    /**
     * Gets the player's points.
     * 
     * @return The player's points
     */
    public PlayerPoints getPoints() {
        return points;
    }

    /**
     * Sets the player's points.
     * 
     * @param points The points to set
     */
    public void setPoints(PlayerPoints points) {
        this.points = points;
    }

    /**
     * Calculates and assigns base points based on game outcome.
     * 
     * @param gameStatus The final status of the game
     */
    public void calculatePoints(GameStatus gameStatus) {
        boolean isWinner = (isRed() && gameStatus == GameStatus.RED_WIN) || 
                          (isBlack() && gameStatus == GameStatus.BLACK_WIN);
        boolean isTie = gameStatus == GameStatus.TIE;

        double basePoints = points.calculateBasePoints(isWinner, isTie);
        points.setBasePoints(basePoints);
    }

    /**
     * Adds Prima Nota bonus points based on the number of correct guesses.
     * 
     * @param correctGuesses The number of correct Prima Nota guesses
     */
    public void addPrimaNotaBonus(int correctGuesses) {
        if (isPrimaNotaPlayer && primaNotaGuesses != null) {
            points.addPrimaNotaBonus(correctGuesses, role == PlayerRole.SHERIFF);
        }
    }

    /**
     * Gets the player's sanctions.
     * 
     * @return The player's sanctions
     */
    public PlayerSanctions getSanctions() {
        return sanctions;
    }

    /**
     * Adds a warning to the player.
     * According to the rules, 3 warnings result in losing the next speech,
     * and 4 warnings result in removal from the game.
     * 
     * @return true if the player should be removed from the game, false otherwise
     */
    public boolean addWarning() {
        boolean shouldRemove = sanctions.addWarning();
        if (shouldRemove) {
            points.addRemovalPenalty();
        }
        return shouldRemove;
    }

    /**
     * Removes the player from the game due to a serious violation.
     */
    public void removeForViolation() {
        sanctions.removePlayer();
        points.addRemovalPenalty();
        setAlive(false);
    }

    /**
     * Issues a yellow card to the player.
     */
    public void issueYellowCard() {
        sanctions.issueYellowCard();
        points.addYellowCardPenalty();
    }

    /**
     * Issues a red card to the player.
     */
    public void issueRedCard() {
        sanctions.issueRedCard();
        points.addRedCardPenalty();
    }

    /**
     * Checks if the player has lost their next speech.
     * According to the rules, this happens when a player has 3 warnings.
     * 
     * @return true if the player has lost their next speech, false otherwise
     */
    public boolean hasLostNextSpeech() {
        return sanctions.hasLostNextSpeech();
    }

    /**
     * Resets the player's state for a new day or night.
     */
    public void resetDailyState() {
        this.hasSpoken = false;
        this.hasVoted = false;
        this.hasChecked = false;
        this.hasNominated = false;
    }

    /**
     * Checks if the player is on the Red team (Citizen or Sheriff).
     * 
     * @return true if the player is on the Red team, false otherwise
     */
    public boolean isRed() {
        return role != null && role.isRed();
    }

    /**
     * Checks if the player is on the Black team (Mafia or Don).
     * 
     * @return true if the player is on the Black team, false otherwise
     */
    public boolean isBlack() {
        return role != null && role.isBlack();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", seatNumber=" + seatNumber +
                ", role=" + role +
                ", alive=" + alive +
                '}';
    }
}
