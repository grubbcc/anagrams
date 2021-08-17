Anagrams is a classic word game of thought and thievery. For many players, including myself, their first encounter with Anagrams occurred at the end of a long day of playing tournament Scrabble™, but it has a long popular history as well. Originating in Victorian England (according to the Wikipedia article), the first published version dates to 1877 or earlier. It has appeared in various tabletop versions over the years, one of which even made its way into a Hitchcock movie:

It can be played with any set of letter tiles, but until recently there has not been any way to play over the web. Now there at least three different ways to play: Jay Chan's mobile-friendly Snatch!, Carl Johnson's browser-based Anagrams Blitz, and the program on this page.

How to Play

Gameplay is simple: A certain number of tiles are placed face down into a pool on the table. As they are turned over one at a time, players attempt to form words out of them. If a player is successful at finding a word, they arrange the tiles to spell out the word and place them in front of themselves. There is no penalty for guessing incorrectly.

Play continues as before, except that now it is possible to steal an existing word. This means adding one or more tiles from the pool to a word to form a longer word. If you steal your opponent's word, it's yours! You may also steal you own words to prevent anyone else from stealing them first.

There is one small catch, however. In order to make a steal, you must rearrange at least two of the tiles. For example it would be possible to steal LAUNCHPAD from CHALUPA, but you could not steal READING to form PREBOARDING because the latter can be spelled by inserting letters into the former without changing their order.

Scoring methods vary, but in general, the player with the most words a the end of the game win. Long words are awarded more points than short ones.

Those are the basics. But wait! There's more. Check out some these other amazing features:

Features

    • Play against up to 5 other players or against the computer (or just watch a game)
    • Automatic dictionary verification with the NWL 18 (North American) and CSW 19 (International) lexicons.
    • Play with up to 300 tiles per game (including "blanks") at adjustable speeds
    * Create a personalized account profile and load customizable settings automatically upon startup
    • Chat with other players before or during a game
    • Postgame analysis mode shows you what words you could have made

A Personal Note

I am an amateur Java programmer, mostly self taught, as well as an avid Scrabble player. I built this project almost entirely from scratch over the course of several years as a way of learning to program and giving back to the word game community. Many, many hours were spent reading the Java Docs and scouring Stack Overflow for answers.

Since I have benefited greatly from the advice and code of others, I have made this an open source project. Check out the code over at my GitHub page, and feel free to use what you like in your own work. (Just no redistributing or selling without my permission, of course.) If you are an experienced prorgammer, any bug reports or feedback on how I can improve things would be greatly appreciated!

Update Log

Version 0.9.9 - August 4, 2021

    • Added text entry widget that checks words as you type.
    • Enlarged buttons and labels for use on mobile phones.
    • Added code to handle screen rotations and full-screen mode on mobile phones.
    • Games pause automatically after a period of activity.

Version 0.9.8 - June 25, 2021

    • Enabled account creation (SSL encrypted) and password login.
    • Enabled personalized profile creation using markdown syntax.
    * Fixed bug that would delay logoff recognition.
    * Chat panel is now resizable.
    * Migrated to faster server.

Version 0.9.7 - February 15, 2021

    • Enabled online play(finall!)
    • Migrated to JavaFX for improved graphical improvement.
    • Added tree visualization widget.
    • Fixed a bug that would cause game panels to disappear prematurely.

Version 0.9.6 - July 6, 2020

    • Added buttons to click through previous game positions.
    • Added a panel that shows all playable words during analysis mode.
    • Word explorer displays probabilities of steals.
    • Game grays out players who have abandoned words on the table.
    • Server keeps track of most commonly missed words.

Version 0.9.5 - June 21, 2020

    • Robot opponents and dictionaries load instantaneously.
    • Customizable appearance and game settings load automatically on startup.
    • Added informative tooltips to the main screen game panels.
    • Names of active players in games are visible from the home screen.
    • Maximum number of windows you can have open at a time is set to 4.

Version 0.9.4 - June 12, 2020

    • Game window can now be maximized and minimized.
    • Added informative tooltips to game menu.
    • Game window now displays game parameters.
    • Tile size automatically adjusts according when panel is full.

Version 0.9.3 - June 10, 2020

    • The game ends immediately and goes to analysis mode if the tile bag and tile pool are both empty.
    • Added NWL18 definitions
    • Players may now select minimum word length of 4 or 5.
    • Game notifications are now displayed in the main screen.
    • You can now resume a game after leaving or getting connected. The game will automatically delete itself
      if there is no activity after one minute.
    • Improved robot loading speed

Version 0.9.2 - June 5, 2020

    • Added chat to the game window
    • Fixed a bug where the dictionary would sometimes fail to load while watching a game
    • Set chat panel to be of fixed size
    • Added sounds for when a player logs in or makes a word
    • Removed the annoying beep when the backspace key is entered in an empty text field

Version 0.9.1 - June 4, 2020

    • You can no longer make a word if it is already on the board
    • Fixed a bug that would sometimes cause socket errors if you quit the application with a game open
    • Created a warning panel that informs the player when their version is out of date
    • Fixed a bug that would prevent player names from being removed if they disconnected unexpectedly
    • Improved the way that game panes tile on the main screen
    • Configured server to run on Dynamic DNS
    • Adjusted Robot capabilities to better match their names

