package com.software.beacon;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;


public class Get_Result extends AsyncTask<String, Void, String> {
    public ValidationResponse delegate;
    public HomeResponse homeDelegate;
    Context ctx;
    String comingFrom = null;
    Get_Result(Context ctx) {
        this.ctx = ctx;
    }
    //int rf;


    @Override
    protected String doInBackground(String... params) {
        String resp = "";
        if(params.length==3)
            if(params[2]!=null) comingFrom = params[2];
        try {
            URL DBUrl = new URL(params[0]);

            HttpURLConnection httpURLConnection = (HttpURLConnection) DBUrl.openConnection();
            httpURLConnection.setReadTimeout(20000);
            httpURLConnection.setConnectTimeout(10000);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);

            String data = URLEncoder.encode("query", "UTF-8") + "=" + URLEncoder.encode(params[1], "UTF-8");
            OutputStream OS = httpURLConnection.getOutputStream();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(OS, "UTF-8"));
            bufferedWriter.write(data);
            bufferedWriter.flush();
            bufferedWriter.close();
            OS.close();

            InputStream IS = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(IS, "iso-8859-1"));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null)
                sb.append(line);
            resp = sb.toString();
            bufferedReader.close();
            IS.close();
            httpURLConnection.disconnect();
        } catch (SocketTimeoutException e) {
            Log.e("SocketTimeoutException", "here");
            resp = "Socket TimedOut! ";
        } catch (MalformedURLException e) {
            Log.e("MalformedException", "here" + e);
            resp = "Malformed URL! ";
        } catch (UnsupportedEncodingException e) {
            Log.e("UEException", "here" + e);
            resp = "Connection Error. Please Try Again!";
        } catch (ProtocolException e) {
            Log.e("ProtocolException", "here" + e);
            resp = "Connection Error. Please Try Again!";
        } catch (IOException e) {
            Log.e("IOException", "here" + e);
            resp = "404 Not Found";
        }
        return resp;
    }

    @Override
    protected void onPostExecute(String s) {
        Log.d("dopost", "here" + s + " "+ctx.getClass());
        boolean isJSON = true;
        if(!s.equals("None") && !s.equals("ok")) {
            try {
                new JSONObject(s);
            } catch (JSONException ex) {
                try {
                    new JSONArray(s);
                } catch (JSONException ex1) {
                    isJSON = false;
                }
            }
        }
        if (s.equals("Connection Error. Please Try Again! ") || s.equals("Socket TimedOut! ") || s.equals("Malformed URL! ") || s.equals("404 Not Found")) {
            if(comingFrom!=null)
                homeDelegate.homeResponse(false,s);
            else delegate.response(false, s);
        }
        else if(!isJSON )
            if(comingFrom!=null) homeDelegate.homeResponse(false, "Database server not reachable !");
            else delegate.response(false, "Database server not reachable !");
        else {
            Log.e("Result", s);
            if (!s.equals("None")) {
                if(comingFrom!=null) homeDelegate.homeResponse(true,s);
                else delegate.response(true, s);
            } else {
                if(comingFrom!=null) homeDelegate.homeResponse(false,s);
                else delegate.response(false, s);
            }
        }
    }
}
