package JavaBeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is File 5 (New File): RecommendationService.java
 * This is the "brain" of the recommendation system.
 * It finds fictions with similar tags.
 */
public class RecommendationService {

    private final List<Work> allWorks;
    private final int MAX_RECOMMENDATIONS = 5;

    /**
     * Constructor: Initializes the service with the full list of works.
     * @param allWorks The complete list of works from the main TableView.
     */
    public RecommendationService(List<Work> allWorks) {
        // Create a new list to avoid any concurrent modification issues
        this.allWorks = new ArrayList<>(allWorks != null ? allWorks : Collections.emptyList());
    }

    /**
     * Gets a list of recommended works based on tag similarity.
     * @param currentWork The work the user just reviewed.
     * @return A list of recommended Work objects.
     */
    public List<Work> getRecommendations(Work currentWork) {
        // 1. Get the tags from the work the user just rated
        Set<String> currentWorkTags = currentWork.getTagsSet();

        // If the current work has no tags, we can't recommend anything.
        if (currentWorkTags.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Create a list to hold potential recommendations with their scores
        List<RecommendationScore> scoredWorks = new ArrayList<>();

        // 3. Iterate through EVERY work in the full list
        for (Work candidateWork : allWorks) {

            // Don't recommend the same story back to the user
            if (candidateWork.getUrl().equals(currentWork.getUrl())) {
                continue;
            }

            // 4. Calculate the similarity score
            Set<String> candidateTags = candidateWork.getTagsSet();
            double score = calculateJaccardSimilarity(currentWorkTags, candidateTags);

            // 5. If there's any similarity, add it to our list
            if (score > 0) {
                scoredWorks.add(new RecommendationScore(candidateWork, score));
            }
        }

        // 6. Sort the list by score (highest first)
        scoredWorks.sort((s1, s2) -> Double.compare(s2.score, s1.score));

        // 7. Return the top N recommendations
        return scoredWorks.stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(RecommendationScore::getWork)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the Jaccard similarity between two sets of tags.
     * This is a common method for recommendation engines.
     * Score = (Number of matching tags) / (Total number of unique tags)
     */
    private double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        // Find the intersection (tags in both sets)
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // Find the union (all unique tags from both sets)
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        // Avoid division by zero
        if (union.isEmpty()) {
            return 0.0;
        }

        // Return the score
        return (double) intersection.size() / union.size();
    }

    /**
     * A private helper class to store a Work and its similarity score.
     */
    private static class RecommendationScore {
        private final Work work;
        private final double score;

        RecommendationScore(Work work, double score) {
            this.work = work;
            this.score = score;
        }

        Work getWork() {
            return work;
        }
    }
}

