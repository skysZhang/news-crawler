package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDAO {
    String getNextLink(String sql) throws SQLException;

    String getNextLinkThenDelete() throws SQLException;

    void updateDatabase(String href, String sql) throws SQLException;

    void insertNewsIntoDatabase(String url, String title, String content) throws SQLException;

    boolean isLinkProcessed(String link) throws SQLException;

}


