package org.example.mafia.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a nomination made during the Day phase.
 * Each nomination tracks who made it, who was nominated, and the votes for/against it.
 */
public class Nomination {
    private final Player nominator;
    private final Player nominee;
    private final Set<Player> voters;
    private boolean withdrawn;
    
    /**
     * Creates a new nomination.
     * 
     * @param nominator The player who made the nomination
     * @param nominee The player who was nominated
     */
    public Nomination(Player nominator, Player nominee) {
        this.nominator = nominator;
        this.nominee = nominee;
        this.voters = new HashSet<>();
        this.withdrawn = false;
    }
    
    /**
     * Gets the player who made the nomination.
     * 
     * @return The nominator
     */
    public Player getNominator() {
        return nominator;
    }
    
    /**
     * Gets the player who was nominated.
     * 
     * @return The nominee
     */
    public Player getNominee() {
        return nominee;
    }
    
    /**
     * Gets the set of players who voted for this nomination.
     * 
     * @return The set of voters
     */
    public Set<Player> getVoters() {
        return voters;
    }
    
    /**
     * Adds a vote for this nomination.
     * 
     * @param voter The player who voted
     * @return true if the vote was added, false if the player had already voted
     */
    public boolean addVote(Player voter) {
        return voters.add(voter);
    }
    
    /**
     * Removes a vote for this nomination.
     * 
     * @param voter The player whose vote to remove
     * @return true if the vote was removed, false if the player had not voted
     */
    public boolean removeVote(Player voter) {
        return voters.remove(voter);
    }
    
    /**
     * Gets the number of votes for this nomination.
     * 
     * @return The vote count
     */
    public int getVoteCount() {
        return voters.size();
    }
    
    /**
     * Checks if this nomination has been withdrawn.
     * 
     * @return true if the nomination has been withdrawn, false otherwise
     */
    public boolean isWithdrawn() {
        return withdrawn;
    }
    
    /**
     * Withdraws this nomination.
     * According to the rules, a player can withdraw their own nomination during their minute.
     */
    public void withdraw() {
        this.withdrawn = true;
    }
    
    @Override
    public String toString() {
        return "Nomination{" +
                "nominator=" + nominator.getSeatNumber() +
                ", nominee=" + nominee.getSeatNumber() +
                ", voteCount=" + getVoteCount() +
                ", withdrawn=" + withdrawn +
                '}';
    }
}