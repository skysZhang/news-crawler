package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class Main {
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:C://Users/Administrator/IdeaProjects/news-crawler/news", "root", "root");

        String link;

        // 从数据库中加载下一个链接，如果能加载到则进行循环
        while ((link = getNextLinkThenDelete(connection)) != null) {
            // 询问数据库，当前链接是不是已经被处理过了
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);

                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);

                storeIntoDatabaseIfItIsNewsPage(doc);

                updateDatabase(connection, link, "insert into LINKS_ALREADY_PROCESSED (link) values (?)");
            }
        }
    }

    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")
        ) {
            String href = aTag.attr("href");
            updateDatabase(connection, href, "insert into LINKS_TO_BE_PROCESSED (link) values (?)");
        }
    }

    private static boolean isLinkProcessed(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select link from LINKS_ALREADY_PROCESSED where link = ?");
        ) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

    private static void updateDatabase(Connection connection, String href, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            statement.setString(1, href);
            statement.executeUpdate();
        }
    }

    private static String getNextLinkThenDelete(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
        ) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                String title = articleTags.get(0).child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        if (link.startsWith("//")) {
            link = "https:" + link;
        }
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Safari/537.36");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            String html = EntityUtils.toString(entity1);
            return Jsoup.parse(html);
        }
    }

    public static String getNextLinkThenDelete(Connection connection) throws SQLException {
        String link = getNextLinkThenDelete(connection, "select link from LINKS_TO_BE_PROCESSED limit 1");
        if (link != null) {
            updateDatabase(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED WHERE link = ?");
        }
        return link;
    }


    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}

