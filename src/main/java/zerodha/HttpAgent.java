package zerodha;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;

public class HttpAgent {

    public static void postAuth(String url, List<NameValuePair> params, List<Header> headers) {
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 1000);
        DefaultHttpClient httpClient = new DefaultHttpClient(httpParams);
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(params));
            for (Header header : headers) {
                httpPost.addHeader(header);
            }
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            HttpResponse httpResponse = httpClient.execute(httpPost);
            int code = httpResponse.getStatusLine().getStatusCode();
            String data = EntityUtils.toString(httpResponse.getEntity());
            System.out.println(httpResponse.getStatusLine());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public static final void get(String url, List<Header> headers) {
        final HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 10000);
        HttpClient httpClient = new DefaultHttpClient(httpParams);
        try {
            HttpGet httpGet = new HttpGet(url);
            for (Header header : headers) {
                httpGet.addHeader(header);
            }
            HttpResponse httpResponse = httpClient.execute(httpGet);
            int code = httpResponse.getStatusLine().getStatusCode();
            String data = EntityUtils.toString(httpResponse.getEntity());
            System.out.println(httpResponse.getStatusLine());
        }
       catch (Exception e) {
           e.printStackTrace();
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public static void getHoldings(){
        String url = "https://kite.zerodha.com/oms/portfolio/holdings";

        List<Header> headers11 = new ArrayList<>();
        headers11.add(new BasicHeader("Authorization", WebViewLogin.getKiteTokenHeader()));

        get(url, headers11);
    }

    public static void placeMISOrder() {
        try{
            List<Header> headers11 = new ArrayList<>();
            headers11.add(new BasicHeader("Authorization", WebViewLogin.getKiteTokenHeader()));

            List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("variety", "regular"));
            nameValuePairs.add(new BasicNameValuePair("exchange", "NFO"));
            nameValuePairs.add(new BasicNameValuePair("tradingsymbol", "NIFTY24APR19600CE"));
            nameValuePairs.add(new BasicNameValuePair("transaction_type",  "BUY"));
            nameValuePairs.add(new BasicNameValuePair("order_type", "LIMIT"));
            nameValuePairs.add(new BasicNameValuePair("quantity", "50"));
            nameValuePairs.add(new BasicNameValuePair("price", "3000"));
            nameValuePairs.add(new BasicNameValuePair("product", "MIS"));
            nameValuePairs.add(new BasicNameValuePair("validity", "DAY"));
            nameValuePairs.add(new BasicNameValuePair("disclosed_quantity", "0"));
            nameValuePairs.add(new BasicNameValuePair("trigger_price", "0"));
            nameValuePairs.add(new BasicNameValuePair("squareoff", "0"));
            nameValuePairs.add(new BasicNameValuePair("stoploss", "0"));
            nameValuePairs.add(new BasicNameValuePair("trailing_stoploss", "0"));
            nameValuePairs.add(new BasicNameValuePair("user_id", "RD1234"));

            postAuth("https://kite.zerodha.com/oms/orders/regular", nameValuePairs, headers11);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}
