package org.mcnative.rolloutserver;

import java.io.IOException;

public class RolloutServerBootstrap {

    public static void main(String[] args) {
        try{
            McNativeRolloutServer server = new McNativeRolloutServer();

            server.start();
        }catch (Exception exception){
            exception.printStackTrace();
            System.out.println("Press enter to continue");

            try {
                System.in.read();
                System.exit(0);
            } catch (IOException ignored) {}
        }
    }
}
