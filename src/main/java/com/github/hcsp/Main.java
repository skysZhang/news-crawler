package com.github.hcsp;

public class Main {
    public static void main(String[] args) {
        CrawlerDAO dao = new MyBatisCrawlerDao();
        for (int i = 0; i < 4; i++) {
            new Crawler(dao).start();
        }
    }
}
