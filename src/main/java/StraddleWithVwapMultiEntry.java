import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import utils.NormalCandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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

public class StraddleWithVwapMultiEntry {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(25);
    private static final AtomicInteger trades = new AtomicInteger(0);

    @SneakyThrows
    public static void main(String[] strings) throws FileNotFoundException {
        Date start = new Date();

        Map<Date, SerenityAlgo> DATE_PROFIT_MAP = new TreeMap<>();
        Map<Date, List<String>> STRING_LIST_MAP = new TreeMap<>();

        PrintWriter printWriter = new PrintWriter(new File("FinalResults/backTestOutput/" + StraddleWithVwapMultiEntry.class.getName()+ "-" + new Date().getTime()));
        printWriter.flush();
        File fileo = new File("FinalResults/input/atm");
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
                    Date date = prepareData(file, TREE_MAP, CE_MAP, PE_MAP);

                    List<NormalCandle> ceNormalCandles = new ArrayList<>();
                    List<NormalCandle> peNormalCandles = new ArrayList<>();

                    SerenityAlgo serenityAlgo = new SerenityAlgo();


                    for (Map.Entry<Date, Date> longLongEntry : TREE_MAP.entrySet()) {
                        ceNormalCandles.add(CE_MAP.get(longLongEntry.getKey().getTime()));
                        peNormalCandles.add(PE_MAP.get(longLongEntry.getKey().getTime()));
                        boolean exited = serenityAlgo.executeTrades(peNormalCandles, ceNormalCandles);
                        if (exited) {
                            break;
                        }
                    }

                    if(serenityAlgo.shortStraddle){
                        throw new RuntimeException("trades still open");
                    }
                    DATE_PROFIT_MAP.put(date, serenityAlgo);
                    STRING_LIST_MAP.put(date, serenityAlgo.tradeLogs);
                    System.out.println("finished for date " + date);
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
        for (Map.Entry<Date, SerenityAlgo> dateDoubleEntry : DATE_PROFIT_MAP.entrySet()) {
            aDouble = aDouble + dateDoubleEntry.getValue().profitLossTotal;
            if (dateDoubleEntry.getValue().profitLossTotal >= 0) {
                profitHits = profitHits + 1;
            }
            if (dateDoubleEntry.getValue().profitLossTotal < 0) {
                lossHits = lossHits + 1;
            }
            totalDays = totalDays + 1;
            printWriter.println(dateDoubleEntry.getKey().toString() + " : " + dateDoubleEntry.getValue().profitLossTotal);
        }
        printWriter.println("total profit = " + aDouble + ", profit hits : " + profitHits + "/" + totalDays + ", loss hits " + lossHits + "/" + totalDays + ", orders : " + trades.get() + " order cost " + (trades.get() * 30));
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
        System.out.println(" time taken in seconds " + (new Date().getTime() - start.getTime()) / 1000);
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


    public static class SerenityAlgo {

        private static final int quantityPerLot = 25;

        private boolean shortStraddle = false;
        private float shortStraddleEntryPrice = 0;
        private float profitLossTotal = 0;

        private float maxProfit = 0;
        private float maxLoss = 0;
        private final List<String> tradeLogs = new ArrayList<>();
        private boolean finalExit = false;

        public boolean executeTrades(List<NormalCandle> normalCandleListPE, List<NormalCandle> normalCandleListCE) {
            List<CloseVolume> closeVolumeList = new ArrayList<>();
            String strikeCE = "";
            String strikePE = "";
            for (int x = 0; x < normalCandleListPE.size(); x++) {
                float close = normalCandleListCE.get(x).getClose() + normalCandleListPE.get(x).getClose();

                float high = normalCandleListCE.get(x).getHigh() + normalCandleListPE.get(x).getLow();
                float low = normalCandleListCE.get(x).getLow() + normalCandleListPE.get(x).getHigh();

                long volume = normalCandleListCE.get(x).getVolume() + normalCandleListPE.get(x).getVolume();
                closeVolumeList.add(new CloseVolume(close, (close + high + low) / 3, volume, normalCandleListCE.get(x).getDate()));
                strikeCE = normalCandleListCE.get(x).getSymbol();
                strikePE = normalCandleListPE.get(x).getSymbol();
            }

            float priceVolumeSum = 0;
            float volumeSum = 0;
            for (int x = 0; x < closeVolumeList.size(); x++) {
                CloseVolume closeVolume = closeVolumeList.get(x);
                priceVolumeSum = priceVolumeSum + closeVolume.getPrice() * closeVolume.getVolume();
                volumeSum = volumeSum + closeVolume.getVolume();
                closeVolume.setVwap(priceVolumeSum / volumeSum);
            }
            float vwap = priceVolumeSum / volumeSum;

            if(!finalExit){
            if (!shortStraddle) {
                if ((closeVolumeList.get(closeVolumeList.size() - 1).getClose() < vwap) && ((closeVolumeList.size() < 2) || closeVolumeList.get(closeVolumeList.size() - 2).getClose() > vwap)) {
                    if (closeVolumeList.get(closeVolumeList.size() - 1).date.getHours() >= 15 && closeVolumeList.get(closeVolumeList.size() - 1).date.getMinutes() >= 15) {
                        return true;
                    } else {
                        shortStraddleEntryPrice = closeVolumeList.get(closeVolumeList.size() - 1).getClose();
                        shortStraddle = true;
                        trades.addAndGet(4);
                        tradeLogs.add("Shorting straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + shortStraddleEntryPrice + " for strike price " + strikeCE + "/" + strikePE);
                    }
                }
            } else {
                float tempPL = quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose());
                if (tempPL > 0) {
                    maxProfit = Math.max(tempPL, maxProfit);
                } else {
                    maxLoss = Math.min(tempPL, maxLoss);
                }

                if (closeVolumeList.get(closeVolumeList.size() - 1).getClose() > vwap * 1.1) {
                    //stoploss of 10% hit
                    float profitLoss = (quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose()));
                    profitLossTotal = profitLossTotal + profitLoss;
                    tradeLogs.add("Exiting Shorting straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + closeVolumeList.get(closeVolumeList.size() - 1).getClose() + " at P/L " + profitLoss);
                    shortStraddle = false;
                    return false;
                } else if (closeVolumeList.get(closeVolumeList.size() - 1).getClose() < vwap * 0.7) {
                    //target of 30% hit
                    float profitLoss = (quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose()));
                    profitLossTotal = profitLossTotal + profitLoss;
                    tradeLogs.add("Exiting Shorting straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + closeVolumeList.get(closeVolumeList.size() - 1).getClose() + " at P/L " + profitLoss);
                    shortStraddle = false;
                    return false;
                } else if (closeVolumeList.get(closeVolumeList.size() - 1).date.getHours() >= 15 && closeVolumeList.get(closeVolumeList.size() - 1).date.getMinutes() >= 15) {
                    //exit at 15:15
                    float profitLoss = (quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose()));
                    profitLossTotal = profitLossTotal + profitLoss;
                    tradeLogs.add("Exiting Shorting straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + closeVolumeList.get(closeVolumeList.size() - 1).getClose() + " at P/L " + profitLoss);
                    shortStraddle = false;
                    return true;
                } else if (tempPL < -3000) {
                    float profitLoss = (quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose()));
                    profitLossTotal = profitLossTotal + profitLoss;
                    tradeLogs.add("Exiting Shorting straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + closeVolumeList.get(closeVolumeList.size() - 1).getClose() + " at P/L " + profitLoss);
                    shortStraddle = false;
                    return true;
                } else if (profitLossTotal < -5000) {
                    float profitLoss = (quantityPerLot * (shortStraddleEntryPrice - closeVolumeList.get(closeVolumeList.size() - 1).getClose()));
                    profitLossTotal = profitLossTotal + profitLoss;
                    tradeLogs.add("Exiting Shorting straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + closeVolumeList.get(closeVolumeList.size() - 1).getClose() + " at P/L " + profitLoss);
                    shortStraddle = false;
                    finalExit = true;
                    return true;
                }
            }
            }
            return false;
        }
    }


    @Getter
    public static class CloseVolume {
        private float price, close;
        private long volume;
        private Date date;

        public CloseVolume(float close, float price, long volume, Date date) {
            this.price = price;
            this.close = close;
            this.volume = volume;
            this.date = date;
        }

        @Setter
        private float vwap;
    }

}
