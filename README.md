## Anagrams

[Click here to play online](https://anagrams.site)

Anagrams is a classic word game of thought and thievery. For many players, including myself, their first encounter with Anagrams occurred at the end of a long day of playing tournament Scrabble™, but it has a long popular history as well. Originating in Victorian England (according to the [Wikipedia article](https://en.wikipedia.org/wiki/Anagrams_(game)), the first published version dates to 1877 or earlier. It has appeared in various tabletop versions over the years, one of which even made its way into a Hitchcock movie:

<p align="center">
  <img src="https://seattlephysicstutor.com/anagrams%20suspicion.jpg" width="300px" height="200px">
</p>

It can be played with any set of letter tiles, but until recently there has not been any way to play over the web. Now there at least three different ways to play: Jay Chan's mobile-friendly [Snatch!](https://snatch.cc), Carl Johnson's browser-based [Anagrams Blitz](https://safe-dusk-44647.herokuapp.com/), and the program on this page.

## How to Play

Gameplay is simple: A certain number of tiles are placed face down into a pool on the table. As they are turned over one at a time, players attempt to form words out of them. If a player is successful at finding a word, they arrange the tiles to spell out the word and place them in front of themselves. There is no penalty for guessing incorrectly.

Play continues as before, except that now it is possible to steal an existing word. This means adding one or more tiles from the pool to a word to form a longer word. If you steal your opponent's word, it's yours! You may also steal you own words to prevent anyone else from stealing them first.

There is one small catch, however. In order to make a steal, you must rearrange at least two of the tiles. For example it would be possible to steal LAUNCHPAD from CHALUPA, but you could not steal READING to form PREBOARDING because the latter can be spelled by inserting letters into the former without changing their order.

In order to reward longer words, your score is equal to the square of the word's length.
                                                                                                  
## Development

##### Prerequisites
1. The Java 17 JDK
2. If developing on Linux, see [here](https://www.jpro.one/docs/current/2.7/PREPARING_LINUX_FOR_JPRO) for information on setting up a development environment, or use the prepared Docker container: `docker pull grubbcc/jpro-base:jdk17-fx17`.
3. Recommended: The GitHub CLI tool. See [here](https://github.com/cli/cli/blob/trunk/docs/install_linux.md) for Linux download instructions.
4. In order to build the webserver: Docker and docker-compose

##### How to develop locally

1. Download/clone repository, e.g. `gh repo clone grubbcc/anagrams -- -b main`.
2. Navigate to `anagrams\AnagramsServer` and execute `.\gradlew run`.
3. In another terminal navigate to `anagrams\AnagramsJPro` and execute `.\gradlew jproRestart`. (For a full list of JPro commands, see [here](https://www.jpro.one/docs/current/2.1/JPRO_COMMANDS).)
4. In a browser tab open `localhost:8079`.
5. To register an account, you will need to enable the email verification service. Edit the `from` and `password` fields in `LoginMenu.java` to match an email account you control.

##### Setting up the webserver
1. In `anagrams\AnagramsServer` run `gradlew distZip`
2. In `anagrams\AnagramsJPro` run `gradlew jproRelease`
3. Extract the generated archives into `anagrams\Production`
4. From `Production`, run `Docker build .`
5. Delete lines 1-50 of `nginx.conf` and uncomment the rest
6. Replace `<hostname>` in `nginx.conf` with your hostname.
7. Run `docker-compose up -d`
