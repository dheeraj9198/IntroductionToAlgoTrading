import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestingVwap {

    /**
     *  Price: (H+L+C)/3. VWAP: sum(Volume * Price) / sum(Volume)
     * @param strings
     * @throws Exception
     */
    public static void main(String[] strings) throws Exception{


        BufferedReader bufferedReaderv = new BufferedReader(new FileReader("vwapTest1"));
        String s = null;

        Long prevCumulativeVolume = 0l;

        float volumeSum = 0;
        float priceVolumeSigma = 0;

        List<Float> vwapList = new ArrayList<Float>();

        boolean bought = false;
        float buyingPrice = 0;

        float lastClose = 0;

        while((s = bufferedReaderv.readLine())!=null){
            //System.out.println(s);
            String[] strings1 = s.replaceAll(" ","").split(",");
            Date date = new Date(Long.parseLong(strings1[0]) * 1000);
            if(date.getDate() == 25){
                Float open = Float.parseFloat(strings1[1]);
                Float high = Float.parseFloat(strings1[2]);
                Float low = Float.parseFloat(strings1[3]);
                Float close = Float.parseFloat(strings1[4]);
                Long volume = Long.parseLong(strings1[5]) - prevCumulativeVolume;
                lastClose = close;
                prevCumulativeVolume = Long.parseLong(strings1[5]);

                volumeSum = volumeSum + volume;

                priceVolumeSigma = priceVolumeSigma + (((high+low+close)/3) * volume);

                Float vwap = priceVolumeSigma / volumeSum;
                vwapList.add(vwap);

                if(!bought) {
                    if (close > vwap * 1.01) {
                        System.out.println(" buy at " + close + " on "+date);
                        bought = true;
                        buyingPrice = close;
                    }
                }else if(bought){
                    if(close >= buyingPrice + 10){
                        System.out.println("selling at "+close+" on "+date);
                        System.out.println(" profit = "+(close - buyingPrice)*50);
                        System.out.println("-----------------------------------------");
                        bought = false;
                    }else if(close <= buyingPrice -10) {
                        System.out.println("selling at "+close+" on "+date);
                        System.out.println(" loss = "+(buyingPrice-close)*50);
                        System.out.println("-----------------------------------------");
                        bought = false;
                    }
                }
            }
        }

        if(bought){
            if(lastClose > buyingPrice){
                System.out.println("selling at "+lastClose+" on end time");
                System.out.println(" profit = "+(lastClose - buyingPrice)*50);
                System.out.println("-----------------------------------------");
                bought = false;
            }else if(lastClose < buyingPrice) {
                System.out.println("selling at "+lastClose+ " on end time");
                System.out.println(" loss = "+(buyingPrice-lastClose)*50);
                System.out.println("-----------------------------------------");
                bought = false;
            }
        }

    }
}


