package utils;

import java.util.Date;

public interface Candle {
    public float getOpen();
    public float getHigh();
    public float getLow();
    public float getClose();
    public String getSymbol();
    public long getToken();
    public Date getDate();
    public long getVolume();
}
