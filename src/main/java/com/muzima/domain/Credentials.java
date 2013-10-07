package com.muzima.domain;

public class Credentials {

    private String serverUrl;
    private String userName;
    private String password;

    public Credentials(String userName, String password, String serverUrl) {
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.password = password;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String[] getCredentialsArray(){
        String[] result = new String[3];
        result[0] = userName;
        result[1] = password;
        result[2] = serverUrl;
        return result;
    }
}
