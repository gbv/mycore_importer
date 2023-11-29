package de.vzg.oai_importer.mycore;

import java.util.Objects;

public class MyCoReTargetConfiguration {

    private String url;

    private String user;

    private String password;

    /**
     * The url of the MyCoRe instance
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * The user to use for authentication
     * @return the user
     */
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * The password to use for authentication
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MyCoReTargetConfiguration that = (MyCoReTargetConfiguration) o;
        return Objects.equals(getUrl(), that.getUrl()) && Objects.equals(getUser(), that.getUser()) && Objects.equals(getPassword(), that.getPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUrl(), getUser(), getPassword());
    }
}
