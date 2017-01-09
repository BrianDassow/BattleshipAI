
/**
 * <p>This exception occurs when you attempt to modify a captain in the model
 * that does not exist. If the specified captain name cannot be found in the
 * BattleshipTableModel this exception will be generated and thrown. It is an
 * unchecked exception that will halt your program if it is not caught and
 * handled.</p>
 *
 * @author Seth Berrier - berriers@uwstout.edu
 *
 * @version SPRING.2013
 */
public final class CaptainNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1;
    private String captainName;

    public CaptainNotFoundException(String captainName) {
        super(captainName);
        this.setCaptainName(captainName);
    }

    public CaptainNotFoundException(String captainName, String message) {
        super(message);
        this.setCaptainName(captainName);
    }

    public CaptainNotFoundException(String captainName, Throwable cause) {
        super(cause);
        this.setCaptainName(captainName);
    }

    public CaptainNotFoundException(String captainName, String message, Throwable cause) {
        super(message, cause);
        this.setCaptainName(captainName);
    }

    public CaptainNotFoundException(String captainName, String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.setCaptainName(captainName);
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }
}
