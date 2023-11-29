package de.vzg.oai_importer.mycore.api;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Base64;

@Data
@AllArgsConstructor
public class MCRRestCredentials {

    private String username;
    private String password;

    public String getBase64Encoded() {
        return Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}
