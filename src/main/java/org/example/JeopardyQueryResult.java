package org.example;

import org.apache.lucene.document.Document;

/**
 * Hold the information for a Jeopardy query result
 * @param document the document that was retrieved
 * @param documentScore the document score for the query
 */
public record JeopardyQueryResult(Document document, double documentScore) {
    public String getTitle() {
        return document.get("title");
    }

    public String getContent() {
        return document.get("content");
    }
}
