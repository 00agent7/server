package org.example.mafia.service;

import org.example.mafia.model.Game;
import org.example.mafia.model.Nomination;
import org.example.mafia.model.Player;
import org.example.mafia.model.PlayerPoints;
import org.example.mafia.model.PlayerSanctions;
import org.example.mafia.model.enums.GamePhase;
import org.example.mafia.model.enums.GameStatus;
import org.example.mafia.model.enums.PlayerRole;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing Mafia games.
 * This service provides methods for creating games, adding players, and processing game actions.
 */
@Service
public class GameService {
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();

    /**
     * Creates a new game.
     * 
     * @return The created game
     */
    public Game createGame() {
        Game game = new Game();
        activeGames.put(game.getId(), game);
        return game;
    }

    /**
     * Gets a game by ID.
     * 
     * @param gameId The ID of the game to get
     * @return The game, or null if not found
     */
    public Game getGame(String gameId) {
        return activeGames.get(gameId);
    }

    /**
     * Adds a player to a game.
     * 
     * @param gameId The ID of the game to add the player to
     * @param playerId The ID of the player to add
     * @param seatNumber The seat number for the player
     * @return The added player, or null if the player could not be added
     */
    public Player addPlayer(String gameId, String playerId, int seatNumber) {
        Game game = getGame(gameId);
        if (game == null) {
            return null;
        }

        if (seatNumber < 1 || seatNumber > 10) {
            return null; // Seat number must be between 1 and 10
        }

        Player player = new Player(playerId, seatNumber);
        boolean added = game.addPlayer(player);
        return added ? player : null;
    }

    /**
     * Assigns roles to players in a game.
     * 
     * @param gameId The ID of the game
     * @return true if roles were assigned, false otherwise
     */
    public boolean assignRoles(String gameId) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.SETUP) {
            return false;
        }

        try {
            game.assignRoles();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Starts a game.
     * 
     * @param gameId The ID of the game to start
     * @return true if the game was started, false otherwise
     */
    public boolean startGame(String gameId) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.SETUP) {
            return false;
        }

        try {
            game.startGame();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Advances a game to the next phase.
     * 
     * @param gameId The ID of the game to advance
     * @return The new phase, or null if the game could not be advanced
     */
    public GamePhase nextPhase(String gameId) {
        Game game = getGame(gameId);
        if (game == null || game.getStatus().isGameOver()) {
            return null;
        }

        game.nextPhase();
        return game.getCurrentPhase();
    }

    /**
     * Creates a nomination during the Day phase.
     * 
     * @param gameId The ID of the game
     * @param nominatorSeat The seat number of the player making the nomination
     * @param nomineeSeat The seat number of the player being nominated
     * @return The created nomination, or null if the nomination is invalid
     */
    public Nomination createNomination(String gameId, int nominatorSeat, int nomineeSeat) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.DAY) {
            return null;
        }

        Player nominator = game.getPlayerBySeat(nominatorSeat);
        Player nominee = game.getPlayerBySeat(nomineeSeat);

        if (nominator == null || nominee == null) {
            return null;
        }

        return game.createNomination(nominator, nominee);
    }

    /**
     * Withdraws a nomination.
     * 
     * @param gameId The ID of the game
     * @param nominationIndex The index of the nomination to withdraw
     * @param playerSeat The seat number of the player attempting to withdraw the nomination
     * @return true if the nomination was withdrawn, false otherwise
     */
    public boolean withdrawNomination(String gameId, int nominationIndex, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.DAY) {
            return false;
        }

        List<Nomination> nominations = game.getNominations();
        if (nominationIndex < 0 || nominationIndex >= nominations.size()) {
            return false;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return false;
        }

        return game.withdrawNomination(nominations.get(nominationIndex), player);
    }

    /**
     * Votes for a nomination.
     * 
     * @param gameId The ID of the game
     * @param nominationIndex The index of the nomination to vote for
     * @param voterSeat The seat number of the player voting
     * @return true if the vote was recorded, false otherwise
     */
    public boolean vote(String gameId, int nominationIndex, int voterSeat) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.VOTING) {
            return false;
        }

        List<Nomination> activeNominations = game.getActiveNominations();
        if (nominationIndex < 0 || nominationIndex >= activeNominations.size()) {
            return false;
        }

        Player voter = game.getPlayerBySeat(voterSeat);
        if (voter == null) {
            return false;
        }

        return game.vote(activeNominations.get(nominationIndex), voter);
    }

    /**
     * Eliminates a player from the game.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to eliminate
     * @return true if the player was eliminated, false otherwise
     */
    public boolean eliminatePlayer(String gameId, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null) {
            return false;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null || !player.isAlive()) {
            return false;
        }

        game.eliminatePlayer(player);
        return true;
    }

    /**
     * Records Prima Nota guesses.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player making the guesses
     * @param guesses An array of seat numbers for the guessed Mafia members
     * @return true if the guesses were recorded, false otherwise
     */
    public boolean recordPrimaNotaGuesses(String gameId, int playerSeat, int[] guesses) {
        Game game = getGame(gameId);
        if (game == null) {
            return false;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return false;
        }

        return game.recordPrimaNotaGuesses(player, guesses);
    }

    /**
     * Checks if a player is the Sheriff.
     * This is used by the Don during the Night phase.
     * 
     * @param gameId The ID of the game
     * @param checkerSeat The seat number of the player doing the checking (must be the Don)
     * @param targetSeat The seat number of the player being checked
     * @return true if the target is the Sheriff, false otherwise, or null if the check is invalid
     */
    public Boolean checkSheriff(String gameId, int checkerSeat, int targetSeat) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.NIGHT_DON) {
            return null;
        }

        Player checker = game.getPlayerBySeat(checkerSeat);
        Player target = game.getPlayerBySeat(targetSeat);

        if (checker == null || target == null || !checker.isAlive() || !target.isAlive()) {
            return null;
        }

        if (checker.getRole() != PlayerRole.DON || checker.hasChecked()) {
            return null;
        }

        checker.setHasChecked(true);
        return target.getRole() == PlayerRole.SHERIFF;
    }

    /**
     * Checks if a player is Mafia.
     * This is used by the Sheriff during the Night phase.
     * 
     * @param gameId The ID of the game
     * @param checkerSeat The seat number of the player doing the checking (must be the Sheriff)
     * @param targetSeat The seat number of the player being checked
     * @return true if the target is Mafia, false otherwise, or null if the check is invalid
     */
    public Boolean checkMafia(String gameId, int checkerSeat, int targetSeat) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.NIGHT_SHERIFF) {
            return null;
        }

        Player checker = game.getPlayerBySeat(checkerSeat);
        Player target = game.getPlayerBySeat(targetSeat);

        if (checker == null || target == null || !checker.isAlive() || !target.isAlive()) {
            return null;
        }

        if (checker.getRole() != PlayerRole.SHERIFF || checker.hasChecked()) {
            return null;
        }

        checker.setHasChecked(true);
        return target.isBlack();
    }

    /**
     * Performs a Mafia kill.
     * This is used by the Mafia during the Night phase.
     * 
     * @param gameId The ID of the game
     * @param mafiaSeats The seat numbers of the Mafia players
     * @param targetSeat The seat number of the player being targeted
     * @return true if the kill was successful, false otherwise
     */
    public boolean mafiaKill(String gameId, List<Integer> mafiaSeats, int targetSeat) {
        Game game = getGame(gameId);
        if (game == null || game.getCurrentPhase() != GamePhase.NIGHT_MAFIA) {
            return false;
        }

        // Verify all mafia players
        List<Player> mafiaPlayers = new ArrayList<>();
        for (int seat : mafiaSeats) {
            Player player = game.getPlayerBySeat(seat);
            if (player == null || !player.isAlive() || !player.isBlack()) {
                return false;
            }
            mafiaPlayers.add(player);
        }

        // Verify target player
        Player target = game.getPlayerBySeat(targetSeat);
        if (target == null || !target.isAlive() || target.isBlack()) {
            return false;
        }

        // Verify all mafia players are participating
        if (mafiaPlayers.size() != game.getAliveMafiaPlayers().size()) {
            return false;
        }

        // Kill the target
        game.eliminatePlayer(target);
        return true;
    }

    /**
     * Gets the game status.
     * 
     * @param gameId The ID of the game
     * @return The game status, or null if the game is not found
     */
    public GameStatus getGameStatus(String gameId) {
        Game game = getGame(gameId);
        return game != null ? game.getStatus() : null;
    }

    /**
     * Gets all active games.
     * 
     * @return A list of all active games
     */
    public List<Game> getActiveGames() {
        return new ArrayList<>(activeGames.values());
    }

    /**
     * Adds a warning to a player.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to warn
     * @return true if the warning was added, false otherwise
     */
    public boolean addWarning(String gameId, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null) {
            return false;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return false;
        }

        boolean shouldRemove = player.addWarning();
        if (shouldRemove) {
            player.setAlive(false);
        }

        return true;
    }

    /**
     * Removes a player from the game due to a serious violation.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to remove
     * @return true if the player was removed, false otherwise
     */
    public boolean removePlayerForViolation(String gameId, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null) {
            return false;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return false;
        }

        player.removeForViolation();
        return true;
    }

    /**
     * Issues a yellow card to a player.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to card
     * @return true if the card was issued, false otherwise
     */
    public boolean issueYellowCard(String gameId, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null) {
            return false;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return false;
        }

        player.issueYellowCard();
        return true;
    }

    /**
     * Issues a red card to a player.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player to card
     * @return true if the card was issued, false otherwise
     */
    public boolean issueRedCard(String gameId, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null) {
            return false;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return false;
        }

        player.issueRedCard();
        return true;
    }

    /**
     * Gets a player's points.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player
     * @return The player's points, or null if the player is not found
     */
    public PlayerPoints getPlayerPoints(String gameId, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null) {
            return null;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return null;
        }

        return player.getPoints();
    }

    /**
     * Gets a player's sanctions.
     * 
     * @param gameId The ID of the game
     * @param playerSeat The seat number of the player
     * @return The player's sanctions, or null if the player is not found
     */
    public PlayerSanctions getPlayerSanctions(String gameId, int playerSeat) {
        Game game = getGame(gameId);
        if (game == null) {
            return null;
        }

        Player player = game.getPlayerBySeat(playerSeat);
        if (player == null) {
            return null;
        }

        return player.getSanctions();
    }

    /**
     * Ends a game.
     * 
     * @param gameId The ID of the game to end
     * @return true if the game was ended, false otherwise
     */
    public boolean endGame(String gameId) {
        Game game = activeGames.remove(gameId);
        return game != null;
    }
}
