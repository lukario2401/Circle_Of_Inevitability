package net.lukario.frogerealm.client;

/**
 * The local player's pathway as the client knows it (synced from the server by CSyncPathwayPacket).
 * Use ClientPathwayData.getPathway() in any client code instead of SoulCore.getPathway(),
 * which only works on the server.
 */
public class ClientPathwayData {

    private static String pathway = "none";

    public static String getPathway() {
        return pathway;
    }

    public static void setPathway(String newPathway) {
        pathway = newPathway == null ? "none" : newPathway;
    }

    public static boolean is(String pathwayName) {
        return pathway.equals(pathwayName);
    }
}
