package com.gramviewer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private RecyclerView recyclerView;
    private MessagesAdapter adapter;
    private EditText searchBar;
    private ProgressBar progress;
    private ImageButton scrollUpButton, scrollDownButton;
    private List<Message> allMessages = new ArrayList<>();
    private final String userName = "N";
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler);
        searchBar = findViewById(R.id.search_bar);
        progress = findViewById(R.id.progress);
        scrollUpButton = findViewById(R.id.button_scroll_up);
        scrollDownButton = findViewById(R.id.button_scroll_down);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessagesAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        
        setupSearch();
        setupScrollButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (allMessages.isEmpty() && !isLoading) {
            checkPermissionAndLoad();
        }
    }
    
    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupScrollButtons() {
        scrollUpButton.setOnClickListener(v -> {
            if (adapter.getItemCount() > 0) recyclerView.smoothScrollToPosition(0);
        });
        
        scrollDownButton.setOnClickListener(v -> {
            if (adapter.getItemCount() > 0) recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
        });
    }

    private void filter(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.updateList(allMessages);
            return;
        }
        String q = query.toLowerCase();
        List<Message> f = new ArrayList<>();
        for (Message m : allMessages) {
            if ((m.text != null && m.text.toLowerCase().contains(q)) ||
                (m.author != null && m.author.toLowerCase().contains(q))) {
                f.add(m);
            }
        }
        adapter.updateList(f);
    }

    private void checkPermissionAndLoad() {
        if (!Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            loadChats();
        }
    }

    private void loadChats() {
        if (isLoading) return;
        isLoading = true;
        progress.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "GramViewer/chats");
                if (!dir.exists()) { return; }
                
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".html"));
                if (files == null || files.length == 0) { return; }
                
                List<File> fileList = new ArrayList<>(List.of(files));
          
                // Sort descending (11, 10, ... 1) so oldest chat logs are parsed first.
                Collections.sort(fileList, (f1, f2) -> {
                    try {
                        int n1 = Integer.parseInt(f1.getName().replaceAll("\\D", ""));
                        int n2 = Integer.parseInt(f2.getName().replaceAll("\\D", ""));
                        return Integer.compare(n2, n1);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });

                allMessages.clear();
                List<Message> parsedMessages = HtmlChatParser.parseFiles(fileList, userName);
                allMessages.addAll(parsedMessages);

                runOnUiThread(() -> {
                    adapter.updateList(allMessages);
                    if (!allMessages.isEmpty()) {
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                });

            } catch (Exception ex) {
                Log.e(TAG, "Error loading chats", ex);
            } finally {
                runOnUiThread(() -> progress.setVisibility(View.GONE));
                isLoading = false;
            }
        }).start();
    }
}