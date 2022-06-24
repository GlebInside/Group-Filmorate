package ru.yandex.practicum.javafilmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.javafilmorate.model.Film;
import ru.yandex.practicum.javafilmorate.storage.LikeStorage;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class RecommendationService {

    private final LikeStorage likeStorage;
    private final FilmService filmService;

    @Autowired
    public RecommendationService(LikeStorage likeStorage, FilmService filmService) {
        this.likeStorage = likeStorage;
        this.filmService = filmService;
    }

    public Collection<Film> getRecommendations(Integer userId) {
        var userLikes = likeStorage.allForUser(userId);
        if (userLikes.size() == 0) {
            return new HashSet<>(filmService.firstFilmsWithCountLike(10));
        }

        var allLikes = likeStorage.allExceptUser(userId);
        var map = new TreeMap<Integer, Set<Integer>>(Collections.reverseOrder());

        for (var u : allLikes.keySet()) {
            var anotherUserLikes = allLikes.get(u);
//            anotherUserLikes: [2,3,4]
            //userLikes: [1,2,3]
            var intersection = new HashSet<>(userLikes);
//            intersection: [1,2,3]
            intersection.retainAll(anotherUserLikes);
//            intersection: [2,3]
            var recommendations = new HashSet<>(anotherUserLikes);
//            recommendations: [2,3,4]
            recommendations.removeAll(userLikes);
//            recommendations: [4]
            var commonLikesCount = intersection.size();
            if(!map.containsKey(commonLikesCount)) {
                map.put(commonLikesCount, new HashSet<>());
            }
            map.get(commonLikesCount).addAll(recommendations); // проверить как создаются сеты
        }


        var result = map
                .values()
                .stream()
                .flatMap(Collection::stream)
                .distinct()
                .map(filmService::findFilmById)
                .collect(Collectors.toUnmodifiableList());
        return result;
    }
}
