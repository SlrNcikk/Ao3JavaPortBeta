package JavaBeta; // Adjust package name if necessary

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryManager {

    private static final String HISTORY_FILE = "search_history.json";
    private static final int MAX_HISTORY_SIZE = 15;
    private final Gson gson = new Gson();

    private List<SearchQuery> searchHistory = new ArrayList<>();

    public HistoryManager() {
        loadHistory();
    }

    private void loadHistory() {
        try {
            if (Files.exists(Paths.get(HISTORY_FILE))) {
                Reader reader = new FileReader(HISTORY_FILE);
                Type listType = new TypeToken<ArrayList<SearchQuery>>(){}.getType();
                searchHistory = gson.fromJson(reader, listType);
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("Error loading search history: " + e.getMessage());
        }
        if (searchHistory == null) {
            searchHistory = new ArrayList<>();
        }
    }

    public void saveHistory() {
        try (Writer writer = new FileWriter(HISTORY_FILE)) {
            gson.toJson(searchHistory, writer);
        } catch (IOException e) {
            System.err.println("Error saving search history: " + e.getMessage());
        }
    }

    public void addQuery(SearchQuery newQuery) {
        if (newQuery.toString().isEmpty()) return; // Don't save empty searches

        // Remove duplicate if it exists
        searchHistory.removeIf(q -> q.toString().equals(newQuery.toString()));

        // Add to the front
        searchHistory.add(0, newQuery);

        // Trim the list
        if (searchHistory.size() > MAX_HISTORY_SIZE) {
            searchHistory = searchHistory.subList(0, MAX_HISTORY_SIZE);
        }

        saveHistory();
    }

    public void deleteQuery(SearchQuery queryToDelete) {
        searchHistory.remove(queryToDelete);
        saveHistory();
    }

    public List<SearchQuery> getHistory() {
        return Collections.unmodifiableList(searchHistory);
    }
}