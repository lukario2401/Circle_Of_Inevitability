package net.lukario.frogerealm.client;

/**
 * The local player's aspect as the client knows it (synced from the server by CSyncAspectPacket).
 * Use ClientAspectData.getAspect() in any client code instead of SoulCore.getAspect(),
 * which only works on the server.
 */
public class ClientAspectData {

    private static String aspect = "none";

    public static String getAspect() {
        return aspect;
    }

    public static void setAspect(String newAspect) {
        aspect = newAspect == null ? "none" : newAspect;
    }

    public static boolean is(String aspectName) {
        return aspect.equals(aspectName);
    }
}
