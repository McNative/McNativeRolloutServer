package org.mcnative.rolloutserver;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import net.pretronic.libraries.utility.Iterators;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

public class ServerAuthenticator {

    private final static Argon2 ARGON = Argon2Factory.create();

    private Collection<ServerCredential> credentials;

    public ServerAuthenticator() {
        this.credentials = new ArrayList<>();
    }

    public void setCredentials(Collection<ServerCredential> credentials){
        this.credentials = credentials;
    }

    public boolean isValid(String id, String secret){
        ServerCredential credential = Iterators.findOne(this.credentials, credential1 -> credential1.id.equals(id));
        if(credential == null) return false;
        return ARGON.verify(credential.hash, secret);
    }

    public static class ServerCredential {

        private final String id;
        private final String hash;

        public ServerCredential(String id, String hash) {
            this.id = id;
            this.hash = hash;
        }

        public String getId() {
            return id;
        }

        public String getHash() {
            return hash;
        }
    }

}
