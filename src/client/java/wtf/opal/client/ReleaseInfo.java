package wtf.opal.client;

public final class ReleaseInfo {

    public static final ReleaseChannel CHANNEL = ReleaseChannel.DEVELOPMENT;
    public static final String VERSION = "6.1-beta.67"; // original 2.0-beta.11 lol

    public enum ReleaseChannel {
        PUBLIC("public"),
        BETA("beta"),
        DEVELOPMENT("development");

        private final String name;

        ReleaseChannel(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
