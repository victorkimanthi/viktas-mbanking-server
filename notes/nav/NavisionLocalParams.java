package nav;

public class NavisionLocalParams {

    private String coreBankingType;
    private String coreBankingUrl;
    private String coreBankingUsername;
    private String coreBankingPassword;
    private String coreBankingNamespaceUrl;
    private String coreBankingLocalPort;

    public String getCoreBankingType() {
        return coreBankingType;
    }

    public String getCoreBankingUrl() {
        return coreBankingUrl;
    }

    public String getCoreBankingUsername() {
        return coreBankingUsername;
    }

    public String getCoreBankingPassword() {
        return coreBankingPassword;
    }

    public String getCoreBankingNamespaceUrl() {
        return coreBankingNamespaceUrl;
    }

    public String getCoreBankingLocalPort() {
        return coreBankingLocalPort;
    }

    public void setCoreBankingType(String coreBankingType) {
        this.coreBankingType = coreBankingType;
    }

    public void setCoreBankingUrl(String coreBankingUrl) {
        this.coreBankingUrl = coreBankingUrl;
    }

    public void setCoreBankingUsername(String coreBankingUsername) {
        this.coreBankingUsername = coreBankingUsername;
    }

    public void setCoreBankingPassword(String coreBankingPassword) {
        this.coreBankingPassword = coreBankingPassword;
    }

    public void setCoreBankingNamespaceUrl(String coreBankingNamespaceUrl) {
        this.coreBankingNamespaceUrl = coreBankingNamespaceUrl;
    }

    public void setCoreBankingLocalPort(String coreBankingLocalPort) {
        this.coreBankingLocalPort = coreBankingLocalPort;
    }

    @Override
    public String toString() {
        return "NavisionLocalParams{" +
                "coreBankingType='" + coreBankingType + '\'' +
                ", coreBankingUrl='" + coreBankingUrl + '\'' +
                ", coreBankingUsername='" + coreBankingUsername + '\'' +
                ", coreBankingPassword='" + coreBankingPassword + '\'' +
                ", coreBankingNamespaceUrl='" + coreBankingNamespaceUrl + '\'' +
                ", coreBankingLocalPort='" + coreBankingLocalPort + '\'' +
                '}';
    }
}
