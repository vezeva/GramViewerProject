package com.gramviewer;

import android.util.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HtmlChatParser {
    private static final String TAG = "HtmlChatParser";

    public static List<Message> parseFiles(List<File> files, String currentUsername) {
        List<Message> allMessages = new ArrayList<>();
        if (files == null) return allMessages;

        for (File file : files) {
            List<Message> messagesFromFile = new ArrayList<>(); // Temporary list for this file's messages
            try {
                Document doc = Jsoup.parse(file, "UTF-8");
                Elements messageElements = doc.select("div._a6-g");

                for (Element msgBlock : messageElements) {
                    Element authorElement = msgBlock.selectFirst("h2");
                    Element textContainer = msgBlock.selectFirst("div._a6-p div");

                    if (authorElement != null && textContainer != null) {
                        String author = authorElement.text();
                        String text = textContainer.text();

                        if (!text.isEmpty()) {
                            boolean isSent = author.equalsIgnoreCase(currentUsername);
                            messagesFromFile.add(new Message(author, text, isSent));
                        }
                    }
                }

                // Reverse the list of messages from this file to make it chronological.
                Collections.reverse(messagesFromFile);
                allMessages.addAll(messagesFromFile);

            } catch (IOException e) {
                Log.e(TAG, "Error parsing file: " + file.getAbsolutePath(), e);
            }
        }
        return allMessages;
    }
}