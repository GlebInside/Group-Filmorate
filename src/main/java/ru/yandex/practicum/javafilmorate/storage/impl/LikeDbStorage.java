package ru.yandex.practicum.javafilmorate.storage.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.javafilmorate.storage.EventStorage;
import ru.yandex.practicum.javafilmorate.storage.LikeStorage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Repository
public class LikeDbStorage implements LikeStorage {
    private final String saveLike = "insert into likes(film_id, user_id) values (?, ?)";
    private final String deleteLike = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
    private final String allForUserQuery = "SELECT film_id FROM likes WHERE user_id = ?";
    private final String allExceptUserQuery = "SELECT * FROM likes WHERE user_id <> ?";

    private final JdbcTemplate jdbcTemplate;
    private final EventStorage eventStorage;

    public LikeDbStorage(JdbcTemplate jdbcTemplate, EventStorage eventStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventStorage = eventStorage;
    }

    @Override
    public void save(int filmId, int userId) {
        jdbcTemplate.update(saveLike,
                filmId,
                userId);
        eventStorage.save(filmId, userId, LocalDateTime.now(), "LIKE", "ADD");
    }

    @Override
    public void delete(int filmId, int userId) {
        jdbcTemplate.update(deleteLike,
                filmId,
                userId);
        eventStorage.save(filmId, userId, LocalDateTime.now(), "LIKE", "REMOVE");
    }

    @Override
    public Set<Integer> allForUser(int userId) {
        return new HashSet<>(jdbcTemplate.query(allForUserQuery, (rs, rowNum) -> rs.getInt("film_id"), userId));
    }

    private Integer getFilm_id(ResultSet rs, Integer i) throws SQLException {
        return rs.getInt("film_id");
    }

    @Override
    public Map<Integer, Set<Integer>> allExceptUser(int userId) {
        var map = new HashMap<Integer, Set<Integer>>();
        jdbcTemplate.query(allExceptUserQuery, rs -> {
            var userId1 = rs.getInt("user_id");
            var filmId = rs.getInt("film_id");
            if (!map.containsKey(userId1)) {
                map.put(userId1, new HashSet<>());
            }
            map.get(userId1).add(filmId);
        }, userId);
        return map;
    }

}
