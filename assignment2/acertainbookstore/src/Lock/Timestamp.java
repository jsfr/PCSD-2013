package Lock;

public class Timestamp {
    private static Long timestamp;
    Timestamp(){
        timestamp = System.currentTimeMillis();
    }
    
    public Long get(){
        return timestamp;
    }
}
