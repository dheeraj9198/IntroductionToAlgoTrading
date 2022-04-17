package utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@Builder
@ToString
public class NormalCandle implements Candle{
    private float open,high,low,close;
    private String symbol;
    private long token,volume;
    private Date date;

    private float atp;

    public float getAverage(){
        return (open + high + low + close)/4;
    }

    public float openCloseAverage(){
        return (open + close)/2;
    }

    public boolean isRed(){
        return close < open;
    }

    public boolean isGreen(){
        return close > open;
    }
}
