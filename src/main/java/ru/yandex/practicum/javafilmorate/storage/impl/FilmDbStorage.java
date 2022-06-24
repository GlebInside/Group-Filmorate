package ru.yandex.practicum.javafilmorate.storage.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.javafilmorate.model.Film;
import ru.yandex.practicum.javafilmorate.model.GENRE;
import ru.yandex.practicum.javafilmorate.model.MPA;
import ru.yandex.practicum.javafilmorate.storage.FilmStorage;
import ru.yandex.practicum.javafilmorate.storage.ReadFilmStorage;

import javax.validation.ValidationException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository("FilmDbStorage")
@Primary
@Slf4j
public class FilmDbStorage implements FilmStorage, ReadFilmStorage {

    private final String deleteFilm = "DELETE FROM films WHERE film_id = ?";
    private final String deleteFilmFromFilmsGenre = "DELETE FROM films_genre WHERE film_id = ?";
    private final String deleteFilmFromLikes = "DELETE FROM likes WHERE film_id = ?";
    private final String deleteFilmGenres = "DELETE FROM films_genre WHERE film_id = ?";
    private final String updateQuery = "update films set name = ?, description = ?, release_date = ?, duration = ?,  mpa = ? where film_id =? ";
    private final String findByIdQuery = "select * from films where film_id = ?";
    private final String getGenreQuery = "SELECT g.name from genres g JOIN films_genre f on g.genre_id = f.genre_id WHERE film_id = ?";
    private final String getUserFilms = "select DISTINCT * from films f join likes l on f.film_id = l.film_id where user_id = ?";
    private final String saveGenreQuery = "insert into films_genre(film_id, genre_id) values(?, ?)";
    private final String getPopularQuery = "SELECT f.film_id, f.name, f.description, f.release_date,f.duration, f.mpa\n" +
            "FROM films AS f LEFT JOIN likes AS l on f.film_id = l.film_id\n" +
            "GROUP BY f.film_id\n" +
            "ORDER BY COUNT(DISTINCT l.user_id) DESC LIMIT ?";
    private final String searchQuery = "select f.*, count(l.film_id) as cnt from films f " +
            "left join likes l on f.film_id = l.film_id " +
            "where f.name ilike ? " +
            "group by f.FILM_ID, f.NAME, f.DESCRIPTION, f.RELEASE_DATE, f.DURATION, f.MPA " +
            "order by cnt desc";


    private final JdbcTemplate jdbcTemplate;
    private final UserDbStorage userDbStorage;

    public FilmDbStorage(JdbcTemplate jdbcTemplate, UserDbStorage userDbStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.userDbStorage = userDbStorage;
    }

    @Override
    public int save(Film film) {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("film_id");
        int filmId = simpleJdbcInsert.executeAndReturnKey(film.toMap()).intValue();
        if (film.getGenres() != null) {
            saveGenre(filmId, film.getGenres());
        }
        return filmId;
    }

    @Override
    public void delete(Integer filmId) {
        jdbcTemplate.update(deleteFilmFromLikes,
                filmId);
        jdbcTemplate.update(deleteFilmFromFilmsGenre,
                filmId);
        jdbcTemplate.update(deleteFilm,
                filmId);
    }

    public void update(Film film) {
        jdbcTemplate.update(updateQuery,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().toString(),
                film.getId());
        if (film.getGenres() != null) {
            deleteGenre(film.getId());
            saveGenre(film.getId(), film.getGenres());
        }
    }

    @Override
    public Collection<Film> returnAllFilms() {
        return jdbcTemplate.query("SELECT * from films", this::makeFilm);
    }

    public Optional<Film> findFilmById(int filmId) {
        return jdbcTemplate.query(
                findByIdQuery, (rs, rowNum) -> makeFilm(rs, rowNum), filmId
        ).stream().findAny();
    }


    private Film makeFilm(ResultSet rs, int rowNum) throws SQLException {
        int filmId = rs.getInt("film_id");

        return new Film(rs.getInt("film_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDate("release_date").toLocalDate(),
                rs.getShort("duration"),
                userDbStorage.findUsersLikeToFilm(filmId),
                MPA.valueOf(rs.getString("mpa")),
                getGenres(filmId)
//                new HashSet<GENRE>()
        );
    }

    public Collection<Film> getPopular(int limit) {
        return jdbcTemplate.query(getPopularQuery, ((rs, rowNum) -> makeFilm(rs, rowNum)), limit);
    }

    private Collection<Film> getUserFilms(int userId) {
        return jdbcTemplate.query(getUserFilms, ((rs, rowNum) -> makeFilm(rs, rowNum)), userId);
    }

    @Override
    public List<Film> getCommonFilms(int userId, int friendId) {
        Collection<Film> userF = getUserFilms(userId);
        Collection<Film> friendF = getUserFilms(friendId);
        return userF.stream()
                .filter(o -> friendF.contains(o)).sorted((o1, o2) -> o2.getUsersLike().size() - o1.getUsersLike().size())
                .collect(Collectors.toList());
    }

    private Collection<GENRE> getGenres(int filmId) {
        Collection<GENRE> genres = jdbcTemplate.query(getGenreQuery,
                (rs, rowNum) -> makeGenre(rs, rowNum),
                filmId);
        if (!genres.isEmpty()) {
            return genres;
        } else {
            return null;
        }
    }

    private void deleteGenre(Integer filmId) {
        jdbcTemplate.update(deleteFilmGenres,
                filmId);
    }

    private boolean findGenre(Integer id) {
        SqlRowSet genreRows = jdbcTemplate.queryForRowSet("select * from genres where genre_id = ?", id);
        if (genreRows.next()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean findGenreForFilm(Integer genreId, Integer filmId) {
        SqlRowSet genreRows = jdbcTemplate.queryForRowSet("select * from films_genre where film_id = ? AND genre_id = ?", filmId, genreId);
        if (genreRows.next()) {
            return true;
        } else {
            return false;
        }
    }

    private void saveGenre(int filmId, Collection<GENRE> genres) {
        if (!genres.isEmpty()) {
            for (GENRE genre : genres) {
                if (findGenre(genre.getId())) {
                    if (!findGenreForFilm(genre.getId(), filmId)) {
                        jdbcTemplate.update(saveGenreQuery,
                                filmId,
                                genre.getId());
                    }
                }
            }
        }
    }

    private GENRE makeGenre(ResultSet rs, int rowNum) throws SQLException {
        String name = rs.getString("name");
        GENRE genre = GENRE.valueOf(name);
        return genre;
    }

    @Override
    public List<Film> search(String query) {
        return jdbcTemplate.query(searchQuery, this::makeFilm, "%" + query + "%");
    }

    @Override
    public Collection<Film> getPopularFilms(int count, int genreId, int year) {
        if (genreId < 0 || year < 0) {
            throw new ValidationException("Cannot be negative");
        }
        if (genreId == 0 && year == 0) {
            return jdbcTemplate.query("SELECT * FROM Films ORDER BY mpa DESC LIMIT ?",this::makeFilm, count);
        } else if (year == 0) {
            return (Collection<Film>) jdbcTemplate.query("SELECT *" +
                    "FROM Films " +
                    "INNER JOIN Films_genre ON Films.film_id = Films_genre.film_id " +
                    "WHERE Films_genre.genre_id = ?" +
                    "ORDER BY Films.mpa DESC " +
                    "LIMIT ?",this::makeFilm, genreId, count);
        } else if (genreId == 0) {
            return (Collection<Film>) jdbcTemplate.query("SELECT * " +
                    "from Films " +
                    "where extract(year from release_date) = ? " +
                    "order by mpa desc " +
                    "limit ?",this::makeFilm, year, count);
        } else {
            return (Collection<Film>) jdbcTemplate.query("select * " +
                    "from Films " +
                    "inner join Films_genre on Films.film_id = Films_genre.film_id " +
                    "where Films_genre.genre_id = ? and extract(year from Films.release_date) = ? " +
                    "order by Films.mpa desc " +
                    "limit ?",this::makeFilm, genreId, year, count);
        }
    }
}