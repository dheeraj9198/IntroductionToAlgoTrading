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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class BuyAlgo {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(1);
    private static final AtomicInteger trades = new AtomicInteger(0);

    @SneakyThrows
    public static void main(String[] strings) throws FileNotFoundException {
        Date start = new Date();

        Map<Date, Float> DATE_PROFIT_MAP = new TreeMap<>();
        Map<Date, List<String>> STRING_LIST_MAP = new TreeMap<>();

        PrintWriter printWriter = new PrintWriter(new File("FinalResults/backTestOutput/" +BuyAlgo.class.getSimpleName()+ "-" + new Date().getTime()));
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

                    DATE_PROFIT_MAP.put(date, serenityAlgo.profitLoss);
                    STRING_LIST_MAP.put(date, serenityAlgo.tradeLogs);
                    if (serenityAlgo.shortStraddle) {
                        trades.addAndGet(2);
                    }
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

        float avgLoss = 0 ;
        float avgProfit = 0;

        for (Map.Entry<Date, Float> dateDoubleEntry : DATE_PROFIT_MAP.entrySet()) {
            aDouble = aDouble + dateDoubleEntry.getValue();
            if (dateDoubleEntry.getValue() > 0) {
                profitHits = profitHits + 1;
                avgProfit  =avgProfit + dateDoubleEntry.getValue();
            }
            if (dateDoubleEntry.getValue() < 0) {
                lossHits = lossHits + 1;
                avgLoss = avgLoss + dateDoubleEntry.getValue();
            }
            totalDays = totalDays + 1;
            printWriter.println(dateDoubleEntry.getKey().toString() + " : " + dateDoubleEntry.getValue());
        }
        printWriter.println("total profit = " + aDouble + ", profit hits : " + profitHits + "/" + totalDays + ", loss hits " + lossHits + "/" + totalDays + ", orders : " + trades.get() + " order cost " + (trades.get() * 40));
        printWriter.println("avg profit = "+(avgProfit/profitHits));
        printWriter.println("avg loss = "+(avgLoss/lossHits));
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
        private String strike;
        private float profitLoss;
        private List<String> tradeLogs = new ArrayList<>();

        public boolean executeTrades(List<NormalCandle> normalCandleListPE, List<NormalCandle> normalCandleListCE) {
            List<CloseVolume> closeVolumeList = new ArrayList<>();
            String strikeCE = "";
            String strikePE = "";

            float cePriceVolumeSum = 0;
            float ceVolumeSum = 0;

            float pePriceVolumeSum = 0;
            float peVolumeSum = 0;

            for (int x = 0; x < normalCandleListPE.size(); x++) {
                cePriceVolumeSum = cePriceVolumeSum + normalCandleListCE.get(x).getVolume() * normalCandleListCE.get(x).getClose();
                pePriceVolumeSum = pePriceVolumeSum + normalCandleListPE.get(x).getVolume() * normalCandleListPE.get(x).getClose();

                ceVolumeSum = ceVolumeSum + normalCandleListCE.get(x).getVolume();
                peVolumeSum = peVolumeSum + normalCandleListPE.get(x).getVolume();

                float close = normalCandleListCE.get(x).getClose() + normalCandleListPE.get(x).getClose();
                long volume = normalCandleListCE.get(x).getVolume() + normalCandleListPE.get(x).getVolume();
                closeVolumeList.add(new CloseVolume(close, close, volume, normalCandleListCE.get(x).getDate()));
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
            float straddleVwap = priceVolumeSum / volumeSum;

            float ceVwap = cePriceVolumeSum/ceVolumeSum;
            float peVwap = pePriceVolumeSum/peVolumeSum;


            float ceClose = normalCandleListCE.get(normalCandleListCE.size()-1).getClose();
            float peClose = normalCandleListPE.get(normalCandleListCE.size()-1).getClose();
            float straddleClose = closeVolumeList.get(closeVolumeList.size() - 1).getClose();

            float all = straddleClose/straddleVwap;
            float ce = ceClose/ceVwap;
            float pe = peClose/peVwap;

            if (!shortStraddle &&  ((all > 1.15) || ce > 1.7 || pe > 1.7)) {
                if(ceClose > ceVwap){
                    shortStraddleEntryPrice = normalCandleListCE.get(normalCandleListCE.size()-1).getClose();
                    shortStraddle = true;
                    strike = strikeCE;
                }else if(peClose > peVwap){
                    shortStraddleEntryPrice = normalCandleListPE.get(normalCandleListPE.size()-1).getClose();
                    shortStraddle = true;
                    strike = strikePE;
                }
                tradeLogs.add("Entering Long straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + shortStraddleEntryPrice + " for strike price " + strike);
            }

            if (shortStraddle) {
                float exitPrice = StringUtils.equals(strike, strikeCE) ? normalCandleListCE.get(normalCandleListCE.size()-1).getClose() : normalCandleListPE.get(normalCandleListCE.size()-1).getClose();
                float exitVwap= StringUtils.equals(strike, strikeCE) ? ceVwap : peVwap;
                float tempPL = -1 * quantityPerLot * (shortStraddleEntryPrice - exitPrice);

                if (closeVolumeList.get(closeVolumeList.size() - 1).getClose() > straddleVwap * 1.6) {
                    profitLoss = -1 * quantityPerLot * (shortStraddleEntryPrice - exitPrice);
                    tradeLogs.add("Exiting Long straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + exitPrice);
                    return true;
                } else if (closeVolumeList.get(closeVolumeList.size() - 1).getClose() < straddleVwap * 1.0 || exitPrice < exitVwap*1.2 || tempPL < -3000) {
                    profitLoss = -1 * quantityPerLot * (shortStraddleEntryPrice - exitPrice);
                    tradeLogs.add("Exiting Long straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + exitPrice);
                    return true;
                } else if (closeVolumeList.get(closeVolumeList.size() - 1).date.getHours() >= 15 && closeVolumeList.get(closeVolumeList.size() - 1).date.getMinutes() >= 15) {
                    profitLoss = -1 * quantityPerLot * (shortStraddleEntryPrice - exitPrice);
                    tradeLogs.add("Exiting Long straddle at " + closeVolumeList.get(closeVolumeList.size() - 1).date + " at price " + exitPrice);
                    return true;
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
