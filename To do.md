## NEW FEATURES
* private messages
* add words up to 21 letters long
* add interactive puzzle mode to anagrams.site/tree
* make game setup screen for choosing players
* add admin tools for creating and scheduling tournaments
* show list of top players for players who have played at least, say, 10 rated games
* allow players to choose scoring method?
* Keep track of who creates (is the "host" of games). Prevent users from creating and abandoning too many games. Send pop up warning accordingly.
* make it so you can see the words that can be stolen to produce a given word, i.e. make the wordExplorer bidirectional
* remove redundancy in play validation?
* store gamechat in ArrayList
* make sure blanks are properly accounted for when checking for rearrangement

## FRONT END
* update highlights immediately when highlight_words is enabled
* try not to randomize the board panels upon endgame
* refactor so that addWord and addWords methods take Word objects as parameters
* improve control panel layout (especially in postgame)
* click on tilepool to randomize? (if done only in postgame, this would be easy)
* make sure that sound only plays once when logging in
* make it so clicking on a window brings it to the front
* show quick pop-up notification in bottom of game panel (e.g. "that word is already on the board")?
* show watchers
* automatically recognize and linkify weblinks
* put user's own name at top of the list (and emphasize it somehow?)
* make links in the markdown guide clickable and make it into an editable sandbox?
* add maximize button to player profiles... maybe not necessarily to fullscreen, but at least larger?
* dropdown menu to sort players list alphabetically or by rating
* pop up messages saying you are already involved in this/a game, you already have a game screen active, etc
* make it so tile size also adjusts in response to resizing wordDisplay
* enable multiple game windows?
* add minimize button?

## BACK END
* does the prob calc take into account letters already in the word? e.g. PLEDGEE -> PEGLEGGED
* add profanity filter?
* keep track of no-longer-being-used robot trees to save for postgame analysis so we don't have to waste server time by recreate them
* add features to administrator tools
* automate scheduled server restarts
* backup server
* ping every few seconds to verify connections?
* wouldn't it be better for the Player's words list be a list of <Word>?
* prune in-game search tree depth?
* consider adding a ServerWorker attribute to Player so it knows to whom to communicate game updates
* check for concurrency issues
* use Java Beans conventions to convert WordTree directly into JSONObject (using GSON?)
* convert Play directly into a JSONObject (using GSON?)
* study memory (over)usage with visualvm

## BUGS
* maximize icon sometimes disappears
* overflow words in gamepanel sometimes hidden
* underhanging letters are clipped

## MOBILE
* when waking phone from sleep, full screen size often goes wrong?... maybe just add runlater...
* swipe gesture to navigate through turns in postgame analysis
* change behavior of back button to exit game?
* widen scrollbars in mobile mode
* make sure as much of username and score as possible fits in game panel
* make it so tapping and holding has the effect of right clicking in word explorer?
* make sure all context menus are at the right zoom level

## STAND ALONE VERSION
* custom dictionaries and tile sets

## OTHER
* write comprehensive user guide
* write blogs describing features for woogles
* update website and changelog
* make list of most commonly missed words and share with subscribers
* write a puzzle book
* what is a word that are anagrams of each other as far as the game of Anagrams is concerned, e.g. POMADE, PROMENADE
* if you like the program suggested donation is a $5