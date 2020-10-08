package com.msm.whether_analysis;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView temptext,humtext,co2context,predictedtext;
    private Spinner scitemspinner;

    private MyTask myTask;

    private Timer timer1;
    private TimerTask _timer1;
    private Timer timer2=new Timer();
    private TimerTask _timer2;

    private ArrayList<HashMap<String,Object>> fvstoragelistmap=new ArrayList<>();
    private ArrayList<HashMap<String,Object>> fvlistmap=new ArrayList<>();
    private ArrayList<String> fvlist =new ArrayList<>();
    private HashMap<String,Object> tempmap=new HashMap<>();


    private int temppos=0;
    private String tempitem="";
    private Double pt,ph,pl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        temptext=findViewById(R.id.temptext);
        humtext=findViewById(R.id.humtext);
        co2context=findViewById(R.id.co2context);
        predictedtext=findViewById(R.id.Predictedtext);

        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        timer1=new Timer();
        progressDialog.dismiss();
        _timer1=new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        myTask=new MyTask();
                        myTask.execute();
                    }
                });
            }
        };
        timer1.scheduleAtFixedRate(_timer1,0 ,10000);

    }

    class MyTask extends AsyncTask<Integer, Integer, HashMap<String,Object>> {

        JSONObject response;

        @Override
        protected HashMap<String,Object> doInBackground(Integer... params) {

            HashMap<String,Object> curstat=new HashMap<>();
            String temp="";
            String hum="";
            String co2con="";
            try {
                response = getJSONObjectFromURL("https://api.thingspeak.com/channels/747257/feeds.json?api_key=8SFHNPQ7BRT6LHSK&results=10"); // calls method to get JSON object

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            try {
                double[] temparr=new double[10];
                double[] humarr=new double[10];
                double[] lightarr=new double[10];
                double[] sizearr=new double[]{1,2,3,4,5,6,7,8,9,10};

                for(int i12=0;i12<10;i12++){
                    temparr[i12]=Double.parseDouble(response.getJSONArray("feeds").getJSONObject(i12).get("field1").toString());
                    humarr[i12]=Double.parseDouble(response.getJSONArray("feeds").getJSONObject(i12).get("field2").toString());
                    lightarr[i12]=Double.parseDouble(response.getJSONArray("feeds").getJSONObject(i12).get("field3").toString());
                }
                LinearRegression tr=new LinearRegression(sizearr,temparr);
                LinearRegression hr=new LinearRegression(sizearr,humarr );
                LinearRegression lr=new LinearRegression(sizearr,lightarr );

                pt=tr.predict(11);
                ph=hr.predict(11);
                pl=lr.predict(11);


                temp = response.getJSONArray("feeds").getJSONObject(response.getJSONArray("feeds").length()-1).get("field1").toString();
                hum=response.getJSONArray("feeds").getJSONObject(response.getJSONArray("feeds").length()-1).get("field2").toString();
                co2con=response.getJSONArray("feeds").getJSONObject(response.getJSONArray("feeds").length()-1).get("field3").toString();
            }catch (JSONException e){
                e.printStackTrace();
            }
            curstat.put("temp",temp);
            curstat.put("hum",hum);
            curstat.put("co2con",co2con );
            BigDecimal ta = new BigDecimal(pt);
            BigDecimal troundOff = ta.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            curstat.put("pt",troundOff);
            BigDecimal ha = new BigDecimal(ph);
            BigDecimal hroundOff = ha.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            curstat.put("ph",hroundOff);
            BigDecimal la = new BigDecimal(pl);
            BigDecimal lroundOff = la.setScale(2, BigDecimal.ROUND_HALF_EVEN);
            curstat.put("pl",lroundOff);

            return curstat;
        }
        @Override
        protected void onPostExecute(HashMap<String,Object> result) {
            temptext.setText(result.get("temp").toString()+" C");
            humtext.setText(result.get("hum").toString());
            co2context.setText(result.get("co2con").toString());
            predictedtext.setText("Predicted Values:\nTemperrature:"+String.valueOf(result.get("pt"))+"\nHumidity:"+String.valueOf(result.get("ph"))+"\nLight:"+String.valueOf(result.get("pl")));


        }
        @Override
        protected void onPreExecute() {
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
        }
    }

    public  JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {


        HttpURLConnection urlConnection = null;

        URL url = new URL(urlString);

        urlConnection = (HttpURLConnection) url.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(10000 /* milliseconds */);
        urlConnection.setConnectTimeout(15000 /* milliseconds */);

        urlConnection.setDoOutput(true);

        urlConnection.connect();

        BufferedReader br=new BufferedReader(new InputStreamReader(url.openStream()));

        char[] buffer = new char[1024];

        String jsonString = new String();

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line+"\n");
        }
        br.close();

        jsonString = sb.toString();

        System.out.println("JSON: " + jsonString);
        urlConnection.disconnect();


        return new JSONObject(jsonString);
    }

    public class LinearRegression {
        private final double intercept, slope;
        private final double r2;
        private final double svar0, svar1;

        /**
         * Performs a linear regression on the data points {@code (y[i], x[i])}.
         *
         * @param  x the values of the predictor variable
         * @param  y the corresponding values of the response variable
         * @throws IllegalArgumentException if the lengths of the two arrays are not equal
         */
        public LinearRegression(double[] x, double[] y) {
            if (x.length != y.length) {
                throw new IllegalArgumentException("array lengths are not equal");
            }
            int n = x.length;

            // first pass
            double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
            for (int i = 0; i < n; i++) {
                sumx  += x[i];
                sumx2 += x[i]*x[i];
                sumy  += y[i];
            }
            double xbar = sumx / n;
            double ybar = sumy / n;

            // second pass: compute summary statistics
            double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
            for (int i = 0; i < n; i++) {
                xxbar += (x[i] - xbar) * (x[i] - xbar);
                yybar += (y[i] - ybar) * (y[i] - ybar);
                xybar += (x[i] - xbar) * (y[i] - ybar);
            }
            slope  = xybar / xxbar;
            intercept = ybar - slope * xbar;

            // more statistical analysis
            double rss = 0.0;      // residual sum of squares
            double ssr = 0.0;      // regression sum of squares
            for (int i = 0; i < n; i++) {
                double fit = slope*x[i] + intercept;
                rss += (fit - y[i]) * (fit - y[i]);
                ssr += (fit - ybar) * (fit - ybar);
            }

            int degreesOfFreedom = n-2;
            r2    = ssr / yybar;
            double svar  = rss / degreesOfFreedom;
            svar1 = svar / xxbar;
            svar0 = svar/n + xbar*xbar*svar1;
        }

        /**
         * Returns the <em>y</em>-intercept &alpha; of the best of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>.
         *
         * @return the <em>y</em>-intercept &alpha; of the best-fit line <em>y = &alpha; + &beta; x</em>
         */
        public double intercept() {
            return intercept;
        }

        /**
         * Returns the slope &beta; of the best of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>.
         *
         * @return the slope &beta; of the best-fit line <em>y</em> = &alpha; + &beta; <em>x</em>
         */
        public double slope() {
            return slope;
        }

        /**
         * Returns the coefficient of determination <em>R</em><sup>2</sup>.
         *
         * @return the coefficient of determination <em>R</em><sup>2</sup>,
         *         which is a real number between 0 and 1
         */
        public double R2() {
            return r2;
        }

        /**
         * Returns the standard error of the estimate for the intercept.
         *
         * @return the standard error of the estimate for the intercept
         */
        public double interceptStdErr() {
            return Math.sqrt(svar0);
        }

        /**
         * Returns the standard error of the estimate for the slope.
         *
         * @return the standard error of the estimate for the slope
         */
        public double slopeStdErr() {
            return Math.sqrt(svar1);
        }

        /**
         * Returns the expected response {@code y} given the value of the predictor
         * variable {@code x}.
         *
         * @param  x the value of the predictor variable
         * @return the expected response {@code y} given the value of the predictor
         *         variable {@code x}
         */
        public double predict(double x) {
            return slope*x + intercept;
        }

        /**
         * Returns a string representation of the simple linear regression model.
         *
         * @return a string representation of the simple linear regression model,
         *         including the best-fit line and the coefficient of determination
         *         <em>R</em><sup>2</sup>
         */
        public String toString() {
            StringBuilder s = new StringBuilder();
            s.append(String.format("%.2f n + %.2f", slope(), intercept()));
            s.append("  (R^2 = " + String.format("%.3f", R2()) + ")");
            return s.toString();
        }

    }

}
