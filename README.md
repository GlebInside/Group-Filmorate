<h1 align="center">Group Filmorate (group project)</h1>

<p align="center">
  
<img src="https://img.shields.io/badge/made%20by-glebinside-blue.svg" >

<img src="https://img.shields.io/badge/java-18-orange.svg">

<img src="https://img.shields.io/github/languages/top/glebinside/StepTracker.svg">

</p>

## Description

**Application database diagram**
![scr 2022-11-09 17 07 52](https://user-images.githubusercontent.com/95642615/200851800-560bcd1c-d046-49ba-9b69-803481a6305c.png)

Backend for a service that works with movies and user ratings

## How to use

### The application has only a backend implemented, so you can check its functionality using, for example, postman:
1. Launch the application in the main method
2. Launch <img src="https://img.shields.io/badge/postman-orange.svg"> and send requests 


## About the project

### Films and users

Filmorate implements adding to friends, removing from friends, displaying a list of mutual friends. Adding and removing likes, displaying the 10 most popular movies by the number of likes.
Additionally, the following features were added to the group project: Recommendations, Reviews, Event Feed, Search, Output of the most popular films by genre and year. My role was to write the recommendation feature.

### SQL query examples
1. Get top 10 films by likes: 

`select title, count(*) from films inner join likes on films.id = likes.film_id group by title order by 2 desc limit 10` 

2. Get incoming friedshipRequests for user with id = @user_id:

`select name from users u inner join friendshiprequest fr on u.id = fr.from where status = 'awaiting' and fr.to = @user_id`


