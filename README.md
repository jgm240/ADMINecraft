# ADMINecraft

Fabric mod (Minecraft 1.21.4, Java 21) for community-run server administration.

## Commands (`/adminecraft`, alias `/ae`)

| Command | Who | What |
|---|---|---|
| `/ae elect <player>` | anyone | Start a 3 minute election to make a player admin |
| `/ae impeach <player>` | anyone | Start a 3 minute election to remove an elected admin |
| `/ae vote yes\|no` | anyone (not the candidate) | Cast your vote |
| `/ae status` | anyone | Show the running election |
| `/ae admins` | anyone | List elected admins |
| `/ae banstick <perma\|1d\|7d\|1mo\|1yr>` | admins | Gives you a Ban Stick |
| `/ae ban <player> <duration>` | admins | Ban without a stick |
| `/ae unban <name>` | admins | Lift a ban |
| `/ae admin add\|remove <player>` | ops (level 3) | Bypass elections |

An election passes when YES > NO and at least half of the other online players voted.
Ops count as admins; admins can't ban each other. Elected admins are saved in the world folder
(`adminecraft_admins.json`); bans use vanilla's ban list, so they survive restarts and expire automatically.

## Build

```
gradle wrapper --gradle-version 8.12   # once
./gradlew build                        # jar in build/libs/
```
Drop the jar (plus Fabric API) in `mods/`.

## GUI (press **I**, rebindable in Controls)

- **Voting** (everyone): pick a player, call an election to make them admin or remove them, vote YES/NO. Live status at the bottom.
- **Admin Tools** (admins): gamemode buttons, Flight-in-Survival toggle, and Ban Sticks (perma/1d/7d/1mo/1yr).
- **Stick Enchants** (admins): hold a Ban Stick and apply level-255 Sharpness, Smite, Bane of Arthropods, Knockback, Fire Aspect, Looting, Sweeping Edge, Unbreaking, Mending, or all at once.

Every GUI action is re-validated on the server, so non-admins can't trigger admin actions by faking packets.
Ban Sticks cancel the hit on players (they ban instead of damage), so enchants matter when hitting mobs.

## Admin chat & testing

- `/ac <message>` (or `/ae chat <message>`) sends to a chat only admins can see. Bare `/ac` toggles it for all your normal chat.
- **Debug Player** (Spawn Eggs tab): a wandering fake player with a hay-bale skin, numbered per world. Hit one with a Ban Stick and admins see
  `Debug player #N got banned for <duration>` in admin chat; the dummy is removed.
