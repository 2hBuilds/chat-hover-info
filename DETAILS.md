# 2h Chat Hover Info

> This is the full description of 2h Chat Hover Info - every figure, setting and rule. The short
> version, with pictures, is the plugin's page on the Plugin Hub (the repository's README).

Hover the words in a chat broadcast - a drop, a quest, a combat task, a pet, a level - and a small card opens with
what that thing is: the price, the alch value and the drop rates of an item, the points of a quest, what a level
unlocks, and the first paragraph of the thing's page on the Old School RuneScape Wiki. A right-click on the same
words adds **Open wiki** and **Copy** to the game's menu. The only change it makes to the chat itself is the colour
of the words a card is about, and that has its own switch.

## What it reads (and never reads)

**Four things, and only these:**

- **Broadcast lines of four chats.** The chat message events of **clan chat, guest clan chat, group ironman clan
  chat and friends chat**, and only their *broadcast* kind - the game's own announcements, such as *"... received a
  drop: Dragon claws (121,453,000 coins)"* or *"... has completed a quest: Dragon Slayer II"*.
- **Your own game lines.** The lines the game writes to you alone: a valuable drop, an untradeable drop, a new
  collection log item, and your own kill count.
- **Your own examine text.** When you pick Examine on an item or a monster, RuneLite tells the plugin the pick, and
  the plugin remembers only which item or monster it was, for two game ticks (about a second). The examine line the
  game writes to you in answer is then read, once, for the card of that thing. The text does not name what it is
  about (an immaculate mole skin examines as *"A pristine mole skin."*), so the pick is the only way to know. Every
  other menu pick is ignored, and nothing about the pick is kept or sent anywhere.
- **The chatbox as it is drawn, under the mouse.** To know which words the pointer is on, the plugin reads the
  chat line widgets under it, and only while the pointer is inside the chat's scroll area. Two answers that
  RuneLite's own **Chat Commands** plugin writes into a line after it has arrived - *"<boss> kill count: N"* and
  *"<boss> personal best: T"* - reach the plugin no other way, so they are read as drawn, when the mouse reaches
  them.

**It never reads what a player types.** Whatever anyone writes in public, private, clan, guest clan, group ironman
or friends chat is never handed to a reader. Only the broadcast kinds and your own game lines are.

The cards of the newest 200 chat lines that have one are kept, in memory only; they are gone when the plugin is
switched off or the client closes.

## The cards

A line gives one card, or two when a drop line names the boss it came from (see *Items*). Its picture is the item's own sprite for an item and for a pet, the wiki's own picture of the page for a boss, monster, raid or chest (see
*Wiki picture*), and the game's own icon for the rest: the quest icon, the sword of a task's tier, a skill's icon, the
diary icon. A boss, monster, raid or chest card shows the clue watch (a personal best) or the combat icon (a kill count
and a drop line's boss) in its picture box until the wiki's picture comes, and when the page has none. Every card ends in
a small grey hint, *Right-click: Open wiki | Copy*.

**Items.** A drop, a stacked drop (*"10 x Black knife"*), a clue item, a new collection log item or a raid's
special loot, whether a clan mate's broadcast or one of your own lines. The card shows the item's **Price** and its
**High alch** value, both from RuneLite's item manager, written the way the game writes a stack (*121M*, *25.2M*,
*123K*, *4,600*); an item with no price or no alch value reads *-*. A stack shows *x10* after the name, the price and
the alch value for the whole stack, and the price of one in grey after the price (*460 each*). Then come the **drop
sources**. The card shows them all, each as the source's name and its rate as the wiki writes it (*1/512*), the better
rate first; the bundled table keeps up to six sources an item, monsters before containers. An item the table has no
source for shows no drop line. The item must be one RuneLite knows by that exact name (capitals do not matter), or there is no card. An item the Grand
Exchange does not list is found through an index of every item name that the plugin builds in the first twenty
seconds or so after you log in, so such a card may take that long to appear. A drop line that says where the drop
came from (*"... received a drop: 45 x Runite ore (456,345 coins) from Duke Sucellus."*) also gives the boss, monster,
raid or chest a quiet card of its own, with its name, the wiki's own picture of it in a 64 px box (the combat icon
stands in the box until the picture comes, and when the page has none), and, for a boss in the bundled boss table, its
**Combat** level, **Hitpoints** and **Max hit** and the **Quest** it belongs to (a fact is shown only when there is
something to say, so a boss with no quest has no *Quest* line), then the wiki's first paragraph. That card is on by
default and has its own switch, *Boss cards*. The pictures have theirs, *Wiki pictures*.

**Quests.** *"... has completed a quest: Dragon Slayer II"*. The card shows the quest's **Difficulty**, **Length** and
**Points** from the bundled table. A quest the table does not hold gets *Quest completed* alone.

**Combat achievements.** *"... has completed a Master combat task: Perfect Zulrah (5 points)."* The card shows the
**Tier**, the **Points** and the **Boss** from the table, with the task's description under them as the wiki writes
it; the picture is the sword of the tier. A task the table does not hold shows its tier alone, from the line.

**Pets.** The three ways the game announces one: *"... has a funny feeling like he's being followed: Pet snakeling at
2,678 killcount."*, *"... feels something weird sneaking into his backpack: Heron at 8,426,283 XP."* and *"... feels
like she's acquired something special: Abyssal orphan"*. The card shows the pet's name as the wiki spells it,
**Pet from** (what it comes from), its **Rate** when the drop table holds one, and **Dropped at** with the count and
its unit from the line when the line gives one. A pet the table does not hold reads *Pet* alone.

**Levels.** *"... has reached Defence level 70."* The card is titled *Defence 70*. **New unlocks** says *4*,
*1* or *None*, and the unlocks follow as a numbered list, one per line, a long one wrapping under its own text, as many
as ten lines, the last cut with *...* when more remain, in the order of the wiki's level-up table for that
skill; all the skills are in it. A level before the table has loaded gets a plain card with the skill and its level.
A combat level line gets no card. The words are the phrase *"Defence level 70"*, and it is a *quiet* card (see *The
underline*).

**XP milestones.** *"... has reached 200,000,000 XP in Cooking."* gives a card naming the figure and the skill, with
**Skill**. It is quiet. A total level line gets no card.

**Achievement diaries.** *"... has completed the Elite Ardougne diary."* The card reads *Ardougne diary, Elite* with
its **Tier**; the area is spelled the wiki's way when the table knows it.

**Personal bests.** *"... has achieved a new Vorkath personal best: 2:28"*. The card reads *Vorkath personal best*
with the **Time** first and then, for a boss in the bundled boss table, its combat level, hitpoints and max hit and the
quest it belongs to; the picture is the wiki's own, with the clue watch in the box until it comes.

**Kill counts.** Your own *"Your King Black Dragon kill count is: 420."* The card reads *King Black Dragon kill
count* with **Kills** first and then, for a boss in the bundled boss table, its combat level, hitpoints and max hit and
the quest it belongs to; the picture is the wiki's own, with the combat icon in the box until it comes. The game words its other
counts the same way - chest, completion, harvest, success, opened, lap and ticket counts - and those cards show
**Count** instead.

**Chat Commands answers.** When a member types *!kc* or *!pb*, RuneLite's Chat Commands plugin swaps the typed
command for its answer, *"Brutus kill count: 343"* or *"Chambers of Xeric personal best: 21:14"*. That swap lands
after every chat event has run, so the plugin reads it from the chatbox as drawn, when the mouse reaches the line.
The text has to be the answer from its first word to its last digit, with only a sender and a colon allowed before
it; a line that goes on after the number matches nothing. These cards are quiet. They carry the boss's combat level,
hitpoints, max hit, quest and picture the way the personal best and kill count cards above do. Because the plugin matches
the drawn words, it cannot tell the answer from someone typing the same words by hand.

**Examine text.** Examine an item in your inventory, the bank, your worn items or on the ground, or a monster, and
hover the examine line the game writes to the chat for that thing's card: an item's is the item card above (price,
alch value, drop sources), a monster's is its boss card (the wiki's picture, the facts the bundled boss table holds for
it, the wiki's first paragraph). The pick is what names the thing, so the line has to follow its pick within two game
ticks, and one pick gives one card (RuneLite's own Examine plugin writes a price line after the game's, and that line
gets none). A noted item or a placeholder gives the item itself. An object gives no card, and only your own examine
lines are read. The line reads either of two ways, set by *Examine line*: the thing's name in front of the text (the
default), *"Law rune: Used for teleport spells."*, with the name alone the hover words; or the text alone, as the game
wrote it, with all of it the hover words. The name is written into the chat line on your screen, as the colouring is, and
only on a line that answers your pick. This has its own switch, *Examine cards*, on by default.

When a bundled table is missing or has not loaded yet (they load a second or two after start-up), a quest, task, pet
or diary card shows what the line alone says, an item card shows no drop line, and a boss card shows no facts. In RuneLite's developer mode only,
the command `::hoverinfo demo` posts sample lines to hover, and `::hoverinfo trace` writes one line per
chat event to RuneLite's log for the developer; in a normal client they do nothing.

## Where the card opens and when

**Two places** (the *Card place* setting), and the card looks a little different in each:

- **Above the cursor** (the default). The card is centred above the pointer, its bottom edge 6 px above it. The
  header runs across the top: the picture (in a 36 x 32 box for an item or a game icon, in a 64 px square box for a
  boss, monster, raid or chest), the bold title with *x10* after it in grey for a stack, and the facts of the card
  (price and alch, or tier and points) at the right end of the same row - or, when they do not fit, under the title,
  wrapped onto as many rows as they need. For an item, one grey line of drops follows, *Drops:* then each source with its rate in white. A
  rule, then the wiki's paragraph (when there is one) across the full width of the card in white, in at most five
  lines (three with *Card width* set to Wide), ending in *...* when it runs on. A rule, then the hint. The card is
  as wide as its content asks, never more than the *Card width* setting allows: 409 px at the default text size
  for Medium, the default, 480 for Wide, 391 for Narrow, all growing with the text size; so a short card is narrow. If there is no room above the pointer it
  opens below it, 18 px under, and it is held inside the game window at the sides.
- **Above the chat.** The card is as wide as the chatbox and pinned above it: its left edge on the chatbox's left
  edge, its bottom edge on the chatbox's top edge (held at the top of the window if it is taller than the room).
  The same header row; the drops on a row of their own, headed *Drops* and spread across the width, all in grey; a
  rule; the paragraph in at most two lines; the hint.

Both are drawn on a solid brown ground with a black edge, a lighter line inside it and the game's own type.

**Three ways to open** (the *Show a card* setting):

- **On hover** (the default). The card opens as soon as the pointer is on its words.
- **While holding a key.** The card opens only while the *Card key* is held and the pointer is on the words. The key
  is **Alt** by default and is set on RuneLite's settings page.
- **On click.** A left click on the words opens the card, and it stays while the pointer stays on those words.
  Moving off them, or clicking anywhere else, closes it, so coming back takes a new click. The click is passed on to
  the game untouched. The delay does not apply: the click opens it at once.

**The delay** (*Card delay*, 0 to 1,000 ms, 0 by default) is how long the pointer must rest on the words before the
card opens. It counts from the moment the pointer came onto the words, so a small move on them does not start it
again. **Text size** (*Card text size*, 8 to 24 px, 16 by default) sets the size of the card's text, and the width
limit grows and shrinks with it.

**Only on the words.** A card shows only while the pointer is on that card's own words - the item's name, the quest's
name, the phrase *"Defence level 70"* - and not elsewhere on the row. A line the game wrapped onto two rows is read
row by row, so the words are found on the row they fall on. Nothing happens outside the chat's scroll area, and a
line scrolled out of view does nothing. Only the newest 200 lines that have a card can open one.

**When the chat moves under the pointer.** If the chat moves under a still mouse (a new line arrives and the rows shift
up), the card stays where it is until you move the mouse 3 px or more, turn the mouse wheel, or leave the chat. Moving
the pointer off the words yourself closes it at once, as always. The plugin listens to the wheel only to know it turned;
it reads nothing else from it and passes it on untouched.

**Wiki paragraph.** With *Wiki descriptions* on, a card shows the first paragraph of its wiki page; until the wiki
has answered, it opens without one and asks again on the next hover. A task and a level with unlocks have their own text.

**Wiki picture.** With *Wiki pictures* on, the card of a boss, monster, raid or chest shows the wiki's own picture of
its page, the page's lead image asked for at 128 px wide and scaled down to fit a 64 px square box, its longer side 64
and nothing cropped (a picture already that small is drawn as it is, centred in the box). The game's icon stands in the
box until the picture comes, and stays there when the page has no picture or has one only as a WebP file, which the
client cannot show. The picture is asked for as the line arrives, like the paragraph, kept for the session in memory, the
200 asked for last, and a name is asked for once.

## The underline

**Hover-reveal.** While the pointer is anywhere on a chat line that has a card, the words of every *quiet* card on
that line are underlined with a 1 px line in the *Hover colour*; a blue word carries no underline, it is its own
signal. This happens in every show mode and before the delay is up, so you can see where the words are. On a line that wrapped, there is a piece on each row the words fall on. A line
whose rows the plugin cannot tell apart (a rank icon wide enough to change where the game wraps) may show its card
but no underline.

**Quiet cards.** Item, quest, task, pet, diary, personal best, own kill count and examine-text words are coloured in the chat (the
*Colour hoverable words* setting). Level and XP phrases, the Chat Commands answers and the boss of a
drop line are *quiet*: the line keeps the game's own colour, so the hover-reveal underline is how they show
themselves.

**Underline level-ups** (off by default) keeps a 1 px line, in the *Underline colour* (grey), under
every quiet phrase on every visible chat line, with the pointer anywhere or nowhere. A Chat Commands answer gets its
line only after the mouse has reached it once.

**Colour hoverable words** (on by default) puts the *Hover colour* (blue) on the words of the marked cards. It adds
colour tags around those words and nothing else, closing back to the line's own colour; a line with no marked card is
left exactly as the game wrote it. It applies to a line as the line arrives, so changing it does not recolour lines
already in the chat. Turning it off leaves the words their usual colour, and the hover-reveal underline then shows
under them too, so a line still tells you where its cards are when you point at it.

## The right-click menu

Right-click on the words of a card and two entries join the game's menu, **Open wiki** on top and **Copy** under it,
each with the card's title in orange as its target. They are RuneLite's own kind of entry: nothing is sent to the game.

- **Open wiki** opens the card's page on the Old School RuneScape Wiki, through RuneLite's own link opener. For an
  item it is the wiki's lookup by the item's id and name, which lands on the item's page.
- **Copy** puts the chat line's text on the clipboard, without its colour tags.

The entries appear in every show mode, whether or not the card has opened yet, and only while the pointer is on the
words of a card. With *Show hover cards* off there are none.

## Settings

Eighteen settings, in the order of RuneLite's settings page. All eighteen are also controls in the sidebar's settings
menu (below).

| Setting | Default | What it does |
|---|---|---|
| Show hover cards | On | Show a card at the mouse when you hover an item in a clan or friends chat line. |
| Card text size | 16 | The size of the card's text, in pixels. From 8 to 24. |
| Colour hoverable words | On | Colour the item, quest, achievement and pet names the cards are about in the chat. |
| Hover colour | Blue (#3B82F6) | The colour of the words a card is about, and of the underline under the ones you hover. |
| Show a card | On hover | When a card opens: as soon as you hover, only while you hold the key below, or when you click the words. Choices: *On hover*, *While holding a key*, *On click*. |
| Card key | Alt | The key to hold when 'Show a card' is 'While holding a key'. It is taken from the game only while the mouse is on a card in that mode; everywhere else it types as usual. |
| Card delay | 0 ms | How long the mouse rests on the words before the card opens. From 0 to 1,000 ms. |
| Wiki descriptions | On | Show the first paragraph of the wiki page on each card. Sends only the thing's name to oldschool.runescape.wiki. |
| Wiki pictures | On | Show the wiki's own picture of a boss, monster, raid or chest on its card. Sends only the thing's name to oldschool.runescape.wiki, then fetches the picture the wiki names. |
| Boss cards | On | On a drop line that says where the drop came from, give the boss, monster, raid or chest its own card, with the wiki's first paragraph. |
| Examine cards | On | Hover the text of an item or monster you examined for its card. |
| Examine line | Item name first | Put the examined item's or monster's name in front of its examine text, and hover the name; or keep the text alone and hover all of it. Choices: *Item name first*, *Description only*. |
| Card place | Above the cursor | Where a card opens: centred above the cursor, or as wide as the chatbox and pinned above it. Choices: *Above the cursor*, *Above the chat*. |
| Card width | Medium | How wide the card at the cursor may grow: wide, or two narrower widths that give the paragraph up to five lines. Choices: *Wide*, *Medium*, *Narrow*. |
| Card time | 4m ago | How a recent card's time reads in the sidebar: how long ago, or the clock time in 24 or 12 hours. Choices: *4m ago*, *14:32*, *2:32 PM*. The clock is your own computer's time zone. |
| Name colour | 2h orange (#DC8A00) | The colour of the player's name in the sidebar's recent cards. |
| Underline level-ups | Off | Keep a line under the level and XP phrases that have a card, whether or not the mouse is on them. |
| Underline colour | Grey (#969696) | The colour of that line. |

**Show hover cards** is the master switch: off, no card opens, no underline is drawn (the hover-reveal and the
always-underline both), the right-click entries are not added, and the wiki is not asked for anything. It does not
undo the colouring of the words, which has its own switch.

**The sidebar.** The plugin adds a sidebar button. Its panel has a fixed top card, the recent cards under it, and the
settings menu.

- The **top card** has the Discord mark and the settings icon at its top-right corner, and under them a **How to use**
  box. Its title is a bar across the card (a shade lighter than the card, lighter again under the mouse, an arrow at its
  right): press anywhere on the bar to fold the box away to the bar (the arrow turns to point right) and press it
  again to open it; the sidebar remembers which, and *Reset to default* opens it again. Under the bar come two lines:
  "Hover the coloured name for its card." in white (the words *coloured name* stand in your *Hover colour*; while
  *Colour hoverable words* is off the line reads "Hover a name in a broadcast for its card.") and "Right-click it for
  the wiki." in grey; then a small grey heading, *WHAT IT WORKS ON*, and under it an icon list of what gets a card,
  five rows, each with the game's own icon, a bold label and a grey rest - *Drops & pets* (clan, friends or your chat;
  the casket icon), *Quests, tasks, diaries* (the quest icon), *Level-ups* and what they unlock (the total
  level icon), *Bosses*, kill counts, PBs (the combat icon) and *Examine* any item or monster (a magnifying glass).
- **Recent cards** lists every chat line that gave a card this session, newest first, up to 5,000, in a list that
  scrolls. Each row has the card's picture and, beside it, the card's title in bold, with when the line came small and
  grey in the row's top right corner - how long ago (*now*, *4m*, *2h*) or, as *Card time* is set, the clock time
  (*14:32* or *2:32 PM*). A title that does not fit beside the time goes on to a second line, and that row is taller
  (a third line never: what is left after two is cut with "..."). Under the title come who the line is about (the
  player's name, or *You* for your own lines and examine lines, in the *Name colour*, the 2h orange at first) and what
  kind of card it gave (*drop*, *medium task*, *collection log*, *level-up*, *pet*, *examined* and so on), the kind in
  full; only where the two do not fit is the kind cut with "...", then the name. The header says how many are kept
  (*12 this session*) and has a **Clear** link that
  empties the list. Hover a row and its card shows beside the sidebar, as it does in game; right-click a row for *Open
  wiki* or *Copy*. The list draws only the rows on screen, a new card is one insert at its top and a row's lines are
  counted once, so a long session stays quick. The list is kept for the session only: it is never written anywhere, and
  it is gone when you turn the plugin off or close the client.
**The settings menu.** Press the settings icon (grey at rest, orange under the mouse) and the menu opens under it;
press it again and it closes. The menu has:

- a **header**: the plugin's name, *Version 0.0.7*, and a row of four marks - Discord, X, the 2hBuilds GitHub profile
  and this plugin's own GitHub page - each opening its page through RuneLite's link opener, then closing the menu;
- seven **switches**: *Show hover cards*, *Colour hoverable words*, *Wiki descriptions*, *Wiki pictures*, *Boss cards*,
  *Examine cards*, *Underline level-ups*;
- five **choices**, each a small submenu that shows its current value: *Show a card*, *Card place*, *Card width*,
  *Examine line*, *Card time*;
- the **card key** box, right under *Show a card*: press it, then press the key to hold; Backspace or Delete clears the
  key, Escape keeps the old one. It is greyed unless *Show a card* is *While holding a key*;
- two **rows**: *Card delay (ms)* and *Card text size* (number boxes, the delay stepping by 100); a number applies as you
  type it, and a number out of range is ignored. No box takes the keyboard until it is pressed, so nothing in the menu
  blinks a caret when it opens;
- three **colours**: *Hover colour*, *Underline colour* and *Name colour*, each a swatch; press it and the menu closes,
  RuneLite's colour picker opens on that colour, beside the swatch you pressed, and the menu comes back when the picker
  closes. The picker paints live: the rows of the recent cards show the *Name colour* as you move through the picker,
  before you press its OK, and a colour set on RuneLite's own settings page shows in the sidebar at once;
- at the bottom, **Reset to default** on the left, which puts all eighteen settings back to their defaults, and **OK**
  on the right, which closes the menu.

Everything in the menu saves itself as you set it, and the menu stays open while you work through the switches; OK,
a link, or a click anywhere outside it closes it. Each is the same setting as on RuneLite's settings page.

## Data and credit

The bundled tables - **quests, combat achievements, pets, achievement diaries, level unlocks, bosses and drop rates** - come
from the **Old School RuneScape Wiki**, https://oldschool.runescape.wiki, and are licensed **CC BY-NC-SA 3.0**
(https://creativecommons.org/licenses/by-nc-sa/3.0/). They are cut down to the fields a card shows and bundled inside
the plugin, so reading them needs no download:

| Table | What it holds | Fetched |
|---|---|---|
| Drop rates | 3,715 items, up to six sources each, with the rate as the wiki writes it | 2026-10-07 |
| Quests, combat achievements, pets, diaries, level unlocks | 213 quests, 655 combat tasks, 71 pets, 12 diary areas, and what is new at each level of all 24 skills | 2026-10-08 |
| Bosses | 62 bosses: combat level, hitpoints, max hit, the quest | 2026-10-08 |

The first paragraphs a card shows come from the same wiki, under the same licence, and are read live (see *Privacy*);
one that runs past the card is cut with *...*. The **price** and the **high alch value** come from RuneLite's item
manager, and the pictures are the game's own sprites, drawn through RuneLite, except the boss pictures, which are the
wiki's own, read live like the paragraphs (see *Privacy*), under the same licence. The Discord, X and GitHub marks in the
sidebar are the brands' own, used as links. The plugin's own code is BSD 2-Clause (see `LICENSE`).

## Privacy

**Everything stays on your machine except the names of wiki pages.** Lines, cards, the words you hover and your settings never leave
it. Settings are kept by RuneLite, in its own configuration; the plugin writes no files of its own.

**The one thing sent:** the **name of a card's wiki page** goes to **oldschool.runescape.wiki**, for two things, each
with its own switch.

- While **Wiki descriptions** is on (it is on by default), to ask for the page's first paragraph. The request is the
  wiki's own
  `api.php?action=query&prop=extracts&exintro=1&explaintext=1&redirects=1&format=json&formatversion=2&titles=<name>`,
  for example `titles=Dragon claws`, and nothing else.
- While **Wiki pictures** is on (it is on by default), for the card of a boss, monster, raid or chest, to ask for the
  page's own picture. The request is
  `api.php?action=query&prop=pageimages&piprop=thumbnail&pithumbsize=128&redirects=1&format=json&formatversion=2&titles=<name>`,
  for example `titles=Duke Sucellus`, and nothing else. The wiki's answer names the picture's own address, and the
  plugin then makes one more request, for that address. It follows the address only when it is on
  oldschool.runescape.wiki and ends in PNG, GIF or JPEG; any other address is not requested.

Both go through RuneLite's own HTTP client, in the background (the game never waits for them), with a User-Agent that
names the plugin and its source: *2h Chat Hover Info (RuneLite plugin; github.com/2hBuilds/chat-hover-info)*. Nothing
about you goes with them: not your name, not the chat line, not the world, nothing about your account. Like any web
request they reach the wiki from your IP address.

- **When it is asked.** As the line with a card arrives, so the paragraph is there by the time you hover. A card not
  asked for then (a Chat Commands answer, or a line that arrived while the switch was off) asks on its first hover.
  Only a card with no description of its own asks.
- **Once per name.** A name is asked for once and the answer is kept in memory, the 500 asked for last; a page that
  failed or has no text is kept as empty too, so it is not asked again.
- **A missing page.** If the wiki says the page does not exist, the plugin asks the wiki's search for that same name
  once and, if the first hit is another page, makes one more request for that page's paragraph, kept under the name
  the card asked by. The title in that last request is the one the wiki itself gave back, and the hit is used only
  when its title shares a word with the name asked for; otherwise the card shows no paragraph.
- **Pictures.** A picture is asked for the same way: as the line arrives, once per name, the answer kept in memory, the
  200 asked for last; a page with no picture, or whose picture is a WebP file, is kept as having none and not asked
  again. At most 1 MB of a picture is read, and a longer one is dropped. A missing page is handled the same way: one
  search, the first hit's picture when its title shares a word with the name, otherwise the game's icon.
- **Capped.** At most 10 names wait for an answer at once, and at most 60 requests start in a minute (the page request
  the search and the picture request each count); a name let go because of the cap is asked for again by a later hover.
- **Turn it off** with *Wiki descriptions* and *Wiki pictures*, on RuneLite's settings page or in the sidebar menu, and
  nothing is sent for what each one covers. The cards then show without a paragraph (apart from a task's and a level's
  own bundled text), and with the game's icon in place of a picture. With *Show hover cards* off nothing is sent
  either: no card, no asking.

**Every link opens through RuneLite's own link opener**, and only when you press it: *Open wiki* (in game, and on a row
of the sidebar's recent cards), and the marks in the sidebar's menu and card.

## Found a bug or have an idea?

Come to the **2hBuilds Discord**: https://discord.gg/nsam4CfWzf - the Discord mark in the sidebar's card, and the
first mark in its settings menu, open it. Bugs can also be filed at
https://github.com/2hBuilds/chat-hover-info/issues.
