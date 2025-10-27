package org.example.mafia.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a nomination made during the Day phase.
 * Each nomination tracks who made it, who was nominated, and the votes for/against it.
 */
@Entity
@Table(name = "nominations")
public class Nomination {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "nominator_id", nullable = false)
    private Player nominator;

    @ManyToOne
    @JoinColumn(name = "nominee_id", nullable = false)
    private Player nominee;

    @ManyToMany
    @JoinTable(
        name = "nomination_votes",
        joinColumns = @JoinColumn(name = "nomination_id"),
        inverseJoinColumns = @JoinColumn(name = "voter_id")
    )
    private Set<Player> voters;

    private boolean withdrawn;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

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
     * No-arg constructor for JPA.
     */
    protected Nomination() {
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
