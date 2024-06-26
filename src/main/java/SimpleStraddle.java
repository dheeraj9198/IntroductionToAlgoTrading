import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import utils.NormalCandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleStraddle {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(10);
    private static final AtomicInteger trades =new AtomicInteger(0);

    private static final String strikes = "atm";

    @SneakyThrows
    public static void main(String[] strings) {
        Date start = new Date();

        Map<Date, Straddle> DATE_PROFIT_MAP = new TreeMap<>();
        Map<Date, List<String>> STRING_LIST_MAP = new TreeMap<>();

        PrintWriter printWriter = new PrintWriter(new File("FinalResults/backTestOutput/"+SimpleStraddle.class.getSimpleName()+"-" +strikes+ "-" + new Date().getTime()));
        printWriter.flush();
        File fileo = new File("FinalResults/input/"+strikes);
        File[] files = fileo.listFiles();
        Arrays.sort(files);

        List<Future> futures = new ArrayList<>();

        for (File file : files) {
            Future future = EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    TreeMap<Date, Date> TREE_MAP = new TreeMap<>();
                    Map<Long, NormalCandle> CE_MAP = new HashMap<>();
                    Map<Long, NormalCandle> PE_MAP = new HashMap<>();
                    Date date = prepareData(file,TREE_MAP,CE_MAP,PE_MAP);

                    List<NormalCandle> ceNormalCandles = new ArrayList<>();
                    List<NormalCandle> peNormalCandles = new ArrayList<>();

                    Straddle straddle = new Straddle();


                    for (Map.Entry<Date, Date> longLongEntry : TREE_MAP.entrySet()) {
                          ceNormalCandles.add(CE_MAP.get(longLongEntry.getKey().getTime()));
                        peNormalCandles.add(PE_MAP.get(longLongEntry.getKey().getTime()));
                        boolean exited = straddle.executeTrades(peNormalCandles,ceNormalCandles);
                        if(exited){
                            break;
                        }
                    }

                    DATE_PROFIT_MAP.put(date,straddle);
                    STRING_LIST_MAP.put(date, straddle.tradeLogs);
                    if(straddle.shortStraddle){
                        trades.addAndGet(4);
                    }
                    System.out.println("finished for date "+date);
                }
            });
            futures.add(future);
        }

        Thread.sleep(5000);

        while (true) {
            boolean alldone = true;
            for (Future future : futures) {
                if (!future.isDone()) {
                    alldone = false;
                }
            }
            if (alldone) {
                break;
            }
            Thread.sleep(1000);
        }


        int totalDays = 0;
        int profitHits = 0;
        int lossHits = 0;

        double aDouble = 0;

        float maxProfit = 0;
        float maxLoss = 0;

        float avgProfit = 0;
        float avgLoss = 0;

        for (Map.Entry<Date, Straddle> dateDoubleEntry : DATE_PROFIT_MAP.entrySet()) {
            aDouble = aDouble + dateDoubleEntry.getValue().profitLoss;
            if (dateDoubleEntry.getValue().profitLoss >= 0) {
                profitHits = profitHits + 1;
                maxProfit = Math.max(dateDoubleEntry.getValue().profitLoss , maxProfit);
                avgProfit = avgProfit + dateDoubleEntry.getValue().profitLoss;
            }
            if (dateDoubleEntry.getValue().profitLoss < 0) {
                lossHits = lossHits + 1;
                maxLoss = Math.min(dateDoubleEntry.getValue().profitLoss , maxLoss);
                avgLoss = avgLoss + dateDoubleEntry.getValue().profitLoss;
            }
            totalDays = totalDays + 1;
            printWriter.println(dateDoubleEntry.getKey().toString() + " : " + dateDoubleEntry.getValue().profitLoss+ " (Max profit : "+dateDoubleEntry.getValue().maxProfit+" AND max loss : "+dateDoubleEntry.getValue().maxLoss+")");
        }
        printWriter.println();
        printWriter.println("total profit = " + aDouble + " ,orders : " + trades.get() + " order cost " + (trades.get() * 40));
        printWriter.println("profit hits : " + profitHits + "/" + totalDays + " (" +(profitHits*100/totalDays)+ "%), loss hits " + lossHits + "/" + totalDays + " ("+(lossHits*100/totalDays)+"%)");
        printWriter.println("Max profit : "+maxProfit+ " Max loss : "+maxLoss);
        printWriter.println("Avg profit : "+avgProfit/totalDays+ " Avg loss : "+avgLoss/totalDays);

        printWriter.flush();
        printWriter.println("_______________________________________________");
        printWriter.println("_______________________________________________");
        printWriter.println("_______________________________________________");
        printWriter.flush();
        for (List<String> ss : STRING_LIST_MAP.values()) {
            for (String s : ss) {
                printWriter.println(s);
            }
            printWriter.println("*************************************************************************************************");
        }
        printWriter.flush();
        System.out.println("_____________________________________________________________");
        System.out.println(" time taken in seconds "+(new Date().getTime() - start.getTime())/1000);
        Thread.sleep(1000);
        System.exit(0);
    }


    @SneakyThrows
    private static Date prepareData(File file, Map TREE_MAP, Map CE_MAP, Map PE_MAP) {
            SimpleDateFormat SIMPLE_DATE_FORMAT_1 = new SimpleDateFormat("dd/MM/yy");
            SimpleDateFormat SIMPLE_DATE_FORMAT_2 = new SimpleDateFormat("dd.MM.yy");
            SimpleDateFormat SIMPLE_DATE_FORMAT_3 = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
            String s = null;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            Date date1 = null;
            String callOrPut = null;

            while ((s = bufferedReader.readLine()) != null) {
                //System.out.println(s);
                if (StringUtils.isBlank(s)) {
                    continue;
                }
                if (date1 == null) {
                    date1 = SIMPLE_DATE_FORMAT_1.parse(s.replaceAll("-", "/"));
                    continue;
                }
                if (s.contains("CE") || s.contains("PE")) {
                    callOrPut = s;
                    continue;
                }

                if (s.contains(SIMPLE_DATE_FORMAT_2.format(date1))) {
                    String[] strings = s.split(",");
                    Date date = SIMPLE_DATE_FORMAT_3.parse(strings[0]);
                    /**
                     * 28.12.21 12:23:59,378.90,378.90,364.00,370.00,31375,932800,361.07
                     * date      time    o h l c xx,xx vwap(atp)
                     */
                    Float open = Float.parseFloat(strings[1]);
                    Float high = Float.parseFloat(strings[2]);
                    Float low = Float.parseFloat(strings[3]);
                    Float close = Float.parseFloat(strings[4]);
                    Float atp = Float.parseFloat(strings[7]);
                    long volume = Long.parseLong(strings[5]);

                    NormalCandle normalCandle = NormalCandle.builder()
                            .open(open).high(high).close(close).low(low).volume(volume).date(date).symbol(callOrPut)
                            .build();

                    if (normalCandle.getSymbol().contains("CE")) {
                        CE_MAP.put(date.getTime(), normalCandle);
                    }
                    if (normalCandle.getSymbol().contains("PE")) {
                        PE_MAP.put(date.getTime(), normalCandle);
                    }

                    TREE_MAP.put(date, date);

                }
            }
            String s1 = "";
            return date1;
    }



    public static class Straddle{

        private static final int quantityPerLot= 25;

        private boolean shortStraddle = false;
        private float shortStraddleEntryPrice = 0;
        private float profitLoss;
        private final List<String> tradeLogs = new ArrayList<>();

        private float maxProfit = 0;
        private float maxLoss = 0;

        public boolean executeTrades(List<NormalCandle> normalCandleListPE, List<NormalCandle> normalCandleListCE) {
            List<CloseVolume> closeVolumeList = new ArrayList<>();
            String strikeCE = "";
            String strikePE = "";
            for (int x = 0; x < normalCandleListPE.size(); x++) {
                float close = normalCandleListCE.get(x).getClose() + normalCandleListPE.get(x).getClose();

                float high = normalCandleListCE.get(x).getHigh() + normalCandleListPE.get(x).getLow();
                float low = normalCandleListCE.get(x).getLow() + normalCandleListPE.get(x).getHigh();

                long volume = normalCandleListCE.get(x).getVolume() + normalCandleListPE.get(x).getVolume();
                closeVolumeList.add(new CloseVolume(close, (close+high+low)/3, volume, normalCandleListCE.get(x).getDate()));
                strikeCE = normalCandleListCE.get(x).getSymbol();
                strikePE = normalCandleListPE.get(x).getSymbol();
            }
            //enter trade
            if (!shortStraddle) {
                    shortStraddleEntryPrice = closeVolumeList.get(closeVolumeList.size() - 1).getClose();
                    shortStraddle = true;
                    tradeLogs.add("Shorting straddle at "+closeVolumeList.get(closeVolumeList.size() - 1).date +" at price "+ shortStraddleEntryPrice +" for strike price "+strikeCE+"/"+strikePE);
            }

            //exit trade
            if(shortStraddle) {
                float tempPL = quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose());
                if(tempPL > 0){
                    maxProfit = Math.max(tempPL, maxProfit);
                }else {
                    maxLoss = Math.min(tempPL, maxLoss);
                }

                if(closeVolumeList.get(closeVolumeList.size()-1).date.getHours() >= 15 && closeVolumeList.get(closeVolumeList.size()-1).date.getMinutes()>=15){
                    //exit at 15:15
                    profitLoss = quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose());
                    tradeLogs.add("Exiting Shorting straddle at "+closeVolumeList.get(closeVolumeList.size() - 1).date +" at price "+ closeVolumeList.get(closeVolumeList.size() - 1).getClose());
                    return true;
                }
            }
            return false;
        }
    }


    @Getter
    public static class CloseVolume {
        private float price,close;
        private long volume;
        private Date date;

        public CloseVolume(float close,float price, long volume, Date date) {
            this.price = price;
            this.close = close;
            this.volume = volume;
            this.date = date;
        }

    }

}
