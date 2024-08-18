package message;

public enum MessageType {
    /**
     *  an update of game state sent from another node
     */
    UPDATE,

    /**
     *  message sent from newly joined node to random node, to request a (possibly outdated) gamestate
     */
    REQUEST_GAMESTATE,

    /**
     *  message sent from a random node to a newly joined node, telling it the primary information
     */
    INIT,

    /**
     * when a node wants to get update from primary
     */
    REQUEST_UPDATE,

    ATTEMPT_MOVE, // sent by a player to primary to attempt to update it's position on the board

    CHECK_ALIVE, // sent by player to a random player to check that it is alive

    UPDATE_DEAD, // sent by player to primary to update that it detected a dead player

    NOMINATE_SECONDARY, // sent by primary to nominate a player to be promoted to secondary

    SYNC_SECONDARY, // send by primary to sync gamestate with secondary

    PRIMARY_DEATH, // when a message is meant for primary but player detects that primary dies, so sends request to secondary instead
}
