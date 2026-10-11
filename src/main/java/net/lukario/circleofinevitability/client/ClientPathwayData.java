package net.lukario.circleofinevitability.client;

/**
 * The local player's pathway as the client knows it (synced from the server by CSyncPathwayPacket).
 * Use ClientPathwayData.getPathway() in any client code instead of SoulCore.getPathway(),
 * which only works on the server.
 */
public class ClientPathwayData {

    private static String pathway = "none";
    private static int sequence = 9;
    private static int digestion = 0;

    public static String getPathway() {
        return pathway;
    }

    public static void setPathway(String newPathway) {
        pathway = newPathway == null ? "none" : newPathway;
    }

    public static int getSequence() {
        return sequence;
    }

    /** Digestion of the current Sequence, whole percent. */
    public static int getDigestion() {
        return digestion;
    }

    public static void setProgress(int newSequence, int newDigestion) {
        sequence = newSequence;
        digestion = newDigestion;
    }

    public static boolean is(String pathwayName) {
        return pathway.equals(pathwayName);
    }
}
