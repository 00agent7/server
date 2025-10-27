package org.example.mafia.model;

import jakarta.persistence.*;
import org.example.mafia.model.enums.GamePhase;
import org.example.mafia.model.enums.GameStatus;
import org.example.mafia.model.enums.PlayerRole;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a Mafia game session.
 * This class manages the game state, including players, nominations, and game flow.
 */
@Entity
@Table(name = "games")
public class Game {
    @Id
    private String id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players;

    @Transient
    private Map<Integer, Player> playersBySeat;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Nomination> nominations;

    @Enumerated(EnumType.STRING)
    private GamePhase currentPhase;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private int dayCount;
    private int nightCount;
    private int consecutiveNoEliminationCount;

    @ManyToOne
    @JoinColumn(name = "current_speaker_id")
    private Player currentSpeaker;

    @ManyToOne
    @JoinColumn(name = "eliminated_player_id")
    private Player eliminatedPlayer;

    @ManyToOne
    @JoinColumn(name = "prima_nota_player_id")
    private Player primaNotaPlayer;

    /**
     * Creates a new game with the given ID.
     * 
     * @param id The unique identifier for the game
     */
    public Game(String id) {
        this.id = id;
        this.startTime = LocalDateTime.now();
        this.players = new ArrayList<>();
        this.playersBySeat = new HashMap<>();
        this.nominations = new ArrayList<>();
        this.currentPhase = GamePhase.SETUP;
        this.status = GameStatus.IN_PROGRESS;
        this.dayCount = 0;
        this.nightCount = 0;
        this.consecutiveNoEliminationCount = 0;
    }

    /**
     * Creates a new game with a random ID.
     */
    public Game() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Adds a player to the game.
     * 
     * @param player The player to add
     * @return true if the player was added, false if the seat is already taken
     */
    public boolean addPlayer(Player player) {
        if (players.size() >= 10) {
            return false; // Maximum 10 players
        }

        if (playersBySeat.containsKey(player.getSeatNumber())) {
            return false; // Seat already taken
        }

        players.add(player);
        playersBySeat.put(player.getSeatNumber(), player);
        return true;
    }

    /**
     * Assigns roles to players.
     * According to the rules, there are 7 Citizens (one is Sheriff) and 3 Mafia (one is Don).
     */
    public void assignRoles() {
        if (players.size() != 10) {
            throw new IllegalStateException("Game must have exactly 10 players to assign roles");
        }

        List<Player> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        // Assign 3 Mafia roles (one Don, two regular Mafia)
        shuffledPlayers.get(0).setRole(PlayerRole.DON);
        shuffledPlayers.get(1).setRole(PlayerRole.MAFIA);
        shuffledPlayers.get(2).setRole(PlayerRole.MAFIA);

        // Assign 7 Citizen roles (one Sheriff, six regular Citizens)
        shuffledPlayers.get(3).setRole(PlayerRole.SHERIFF);
        for (int i = 4; i < 10; i++) {
            shuffledPlayers.get(i).setRole(PlayerRole.CITIZEN);
        }
    }

    /**
     * Checks if roles have been assigned to players.
     * 
     * @return true if roles have been assigned, false otherwise
     */
    public boolean areRolesAssigned() {
        if (players.isEmpty()) {
            return false;
        }

        // Check if any player has a non-null role
        return players.stream().anyMatch(player -> player.getRole() != null);
    }

    /**
     * Starts the game.
     * This method assigns roles (if not already assigned) and sets the game phase to NIGHT_0.
     */
    public void startGame() {
        if (players.size() != 10) {
            throw new IllegalStateException("Game must have exactly 10 players to start");
        }

        // Only assign roles if they haven't been assigned yet
        if (!areRolesAssigned()) {
            assignRoles();
        }

        currentPhase = GamePhase.NIGHT_0;
        currentSpeaker = playersBySeat.get(1); // Start with player in seat 1
    }

    /**
     * Advances the game to the next phase.
     */
    public void nextPhase() {
        GamePhase nextPhase = currentPhase.getNextPhase();

        // Handle phase transitions
        if (currentPhase == GamePhase.NIGHT_RESULT && nextPhase == GamePhase.DAY) {
            dayCount++;
            // Rotate the starting speaker
            rotateSpeaker();
        } else if (currentPhase == GamePhase.VOTING && nextPhase == GamePhase.NIGHT_MAFIA) {
            nightCount++;
            // Reset player states for the new night
            players.forEach(Player::resetDailyState);
        }

        // Check for game end conditions
        checkGameEndConditions();

        // If game is over, don't advance to the next phase
        if (status.isGameOver()) {
            currentPhase = GamePhase.GAME_OVER;
            endTime = LocalDateTime.now();
            return;
        }

        currentPhase = nextPhase;
    }

    /**
     * Rotates the starting speaker for the Day phase.
     * According to the rules, the starting speaker rotates forward each Day.
     */
    private void rotateSpeaker() {
        int currentSeatNumber = currentSpeaker.getSeatNumber();
        int nextSeatNumber = (currentSeatNumber % 10) + 1;

        // Find the next alive player
        while (!playersBySeat.get(nextSeatNumber).isAlive()) {
            nextSeatNumber = (nextSeatNumber % 10) + 1;

            // If we've gone full circle, break to avoid infinite loop
            if (nextSeatNumber == currentSeatNumber) {
                break;
            }
        }

        currentSpeaker = playersBySeat.get(nextSeatNumber);
    }

    /**
     * Checks if the game has ended.
     * According to the rules, the game ends when:
     * 1. All Mafia are eliminated (Red win)
     * 2. Mafia count equals or exceeds Citizen count (Black win)
     * 3. 3 Nights + 3 Days pass with no eliminations (Tie)
     */
    private void checkGameEndConditions() {
        // Count alive players by team
        long aliveRedCount = players.stream()
                .filter(Player::isAlive)
                .filter(Player::isRed)
                .count();

        long aliveBlackCount = players.stream()
                .filter(Player::isAlive)
                .filter(Player::isBlack)
                .count();

        // Check win conditions
        GameStatus previousStatus = status;
        if (aliveBlackCount == 0) {
            status = GameStatus.RED_WIN;
        } else if (aliveBlackCount >= aliveRedCount) {
            status = GameStatus.BLACK_WIN;
        } else if (consecutiveNoEliminationCount >= 3) {
            status = GameStatus.TIE;
        }

        // If the game has ended, calculate points
        if (status.isGameOver() && previousStatus != status) {
            calculatePoints();
        }
    }

    /**
     * Calculates and assigns points to all players based on the game outcome.
     * According to the rules, players earn points based on their role, game outcome,
     * and special actions or performances.
     */
    private void calculatePoints() {
        // Calculate base points for all players
        for (Player player : players) {
            player.calculatePoints(status);
        }

        // Calculate Prima Nota bonus points
        if (primaNotaPlayer != null && primaNotaPlayer.getPrimaNotaGuesses() != null) {
            int correctGuesses = getCorrectPrimaNotaGuessCount();
            primaNotaPlayer.addPrimaNotaBonus(correctGuesses);
        }
    }

    /**
     * Creates a nomination during the Day phase.
     * 
     * @param nominator The player making the nomination
     * @param nominee The player being nominated
     * @return The created nomination, or null if the nomination is invalid
     */
    public Nomination createNomination(Player nominator, Player nominee) {
        if (currentPhase != GamePhase.DAY) {
            return null; // Can only nominate during Day phase
        }

        if (!nominator.isAlive() || !nominee.isAlive()) {
            return null; // Both players must be alive
        }

        if (nominator.hasNominated()) {
            return null; // Player can only nominate once per Day
        }

        Nomination nomination = new Nomination(nominator, nominee);
        nominations.add(nomination);
        nominator.setHasNominated(true);
        return nomination;
    }

    /**
     * Withdraws a nomination.
     * According to the rules, a player can withdraw their own nomination during their minute.
     * 
     * @param nomination The nomination to withdraw
     * @param player The player attempting to withdraw the nomination
     * @return true if the nomination was withdrawn, false otherwise
     */
    public boolean withdrawNomination(Nomination nomination, Player player) {
        if (currentPhase != GamePhase.DAY) {
            return false; // Can only withdraw during Day phase
        }

        if (!nominations.contains(nomination)) {
            return false; // Nomination must exist
        }

        if (!nomination.getNominator().equals(player)) {
            return false; // Only the nominator can withdraw
        }

        nomination.withdraw();
        return true;
    }

    /**
     * Votes for a nomination.
     * 
     * @param nomination The nomination to vote for
     * @param voter The player voting
     * @return true if the vote was recorded, false otherwise
     */
    public boolean vote(Nomination nomination, Player voter) {
        if (currentPhase != GamePhase.VOTING) {
            return false; // Can only vote during Voting phase
        }

        if (!voter.isAlive()) {
            return false; // Only alive players can vote
        }

        if (voter.hasVoted()) {
            return false; // Player can only vote once per Voting phase
        }

        if (nomination.isWithdrawn()) {
            return false; // Cannot vote for withdrawn nominations
        }

        boolean added = nomination.addVote(voter);
        if (added) {
            voter.setHasVoted(true);
        }
        return added;
    }

    /**
     * Eliminates a player from the game.
     * 
     * @param player The player to eliminate
     */
    public void eliminatePlayer(Player player) {
        if (!player.isAlive()) {
            return; // Player is already eliminated
        }

        player.setAlive(false);
        eliminatedPlayer = player;

        // Check if this is the first killed player (for Prima Nota)
        if (nightCount == 1 && dayCount == 0 && primaNotaPlayer == null) {
            primaNotaPlayer = player;
            player.setPrimaNotaPlayer(true);
        }

        // Reset consecutive no elimination counter
        consecutiveNoEliminationCount = 0;
    }

    /**
     * Records Prima Nota guesses.
     * According to the rules, the First-Killed Player may name three probable Blacks.
     * 
     * @param player The player making the guesses (must be the Prima Nota player)
     * @param guesses An array of seat numbers for the guessed Mafia members
     * @return true if the guesses were recorded, false otherwise
     */
    public boolean recordPrimaNotaGuesses(Player player, int[] guesses) {
        if (!player.equals(primaNotaPlayer)) {
            return false; // Only the Prima Nota player can make guesses
        }

        if (guesses.length != 3) {
            return false; // Must guess exactly 3 players
        }

        player.setPrimaNotaGuesses(guesses);
        return true;
    }

    /**
     * Gets the number of correct Prima Nota guesses.
     * 
     * @return The number of correct guesses, or -1 if no Prima Nota guesses were made
     */
    public int getCorrectPrimaNotaGuessCount() {
        if (primaNotaPlayer == null || primaNotaPlayer.getPrimaNotaGuesses() == null) {
            return -1;
        }

        int correctCount = 0;
        for (int seatNumber : primaNotaPlayer.getPrimaNotaGuesses()) {
            Player guessedPlayer = playersBySeat.get(seatNumber);
            if (guessedPlayer != null && guessedPlayer.isBlack()) {
                correctCount++;
            }
        }

        return correctCount;
    }

    /**
     * Gets all active (non-withdrawn) nominations.
     * 
     * @return A list of active nominations
     */
    public List<Nomination> getActiveNominations() {
        return nominations.stream()
                .filter(n -> !n.isWithdrawn())
                .collect(Collectors.toList());
    }

    /**
     * Gets all alive players.
     * 
     * @return A list of alive players
     */
    public List<Player> getAlivePlayers() {
        return players.stream()
                .filter(Player::isAlive)
                .collect(Collectors.toList());
    }

    /**
     * Gets all alive Mafia players.
     * 
     * @return A list of alive Mafia players
     */
    public List<Player> getAliveMafiaPlayers() {
        return players.stream()
                .filter(Player::isAlive)
                .filter(Player::isBlack)
                .collect(Collectors.toList());
    }

    /**
     * Gets the Don player.
     * 
     * @return The Don player, or null if not found
     */
    public Player getDonPlayer() {
        return players.stream()
                .filter(p -> p.getRole() == PlayerRole.DON)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the Sheriff player.
     * 
     * @return The Sheriff player, or null if not found
     */
    public Player getSheriffPlayer() {
        return players.stream()
                .filter(p -> p.getRole() == PlayerRole.SHERIFF)
                .findFirst()
                .orElse(null);
    }

    // Getters

    public String getId() {
        return id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public Player getPlayerBySeat(int seatNumber) {
        return playersBySeat.get(seatNumber);
    }

    public List<Nomination> getNominations() {
        return Collections.unmodifiableList(nominations);
    }

    public GamePhase getCurrentPhase() {
        return currentPhase;
    }

    public GameStatus getStatus() {
        return status;
    }

    public int getDayCount() {
        return dayCount;
    }

    public int getNightCount() {
        return nightCount;
    }

    public Player getCurrentSpeaker() {
        return currentSpeaker;
    }

    public Player getEliminatedPlayer() {
        return eliminatedPlayer;
    }

    public Player getPrimaNotaPlayer() {
        return primaNotaPlayer;
    }

    @Override
    public String toString() {
        return "Game{" +
                "id='" + id + '\'' +
                ", currentPhase=" + currentPhase +
                ", status=" + status +
                ", dayCount=" + dayCount +
                ", nightCount=" + nightCount +
                ", alivePlayers=" + getAlivePlayers().size() +
                '}';
    }
}
