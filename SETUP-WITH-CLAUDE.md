# Setup runbook for Claude

> **Human:** give Claude this whole repository (or its link) and say
> *"Set up Horizon Home Kit on my TV using SETUP-WITH-CLAUDE.md."*
> Claude works with access to your PC (the Claude desktop app with your folder connected, or Claude Code).
> It will ask you for the few things only you can provide (keys, logins, a button press on the TV).

---

## Rules for Claude

1. **Ask before every change to the TV**, say what it does and how to undo it. Every script backs up the original value the first time it runs; the matching undo script restores it.
2. **Keys stay private.** Put the user's keys only into `windows/config.bat` on their PC (and in addon links they paste into their own Nuvio account). Never commit them, never paste them into chat summaries, never put them in screenshots or in this repo.
3. Never type passwords or verification codes for the user; ask them to do it.
4. Do one part at a time and check it worked (a TV screenshot via `adb exec-out screencap -p`) before moving on.
5. If the user's PC isn't Windows, run the same `adb` commands the `.bat` files contain directly.

## 0. Ask first

Ask the user, in one message:
- What TV device? (Android TV / Google TV / XGIMI projector / other). Android 8+ is needed for Horizon Home.
- Which parts do they want?
  - **A. Horizon Home** — the ad-free home screen (with the Live TV guide)
  - **B. Live TV addon** for Nuvio (their own free Cloudflare Worker)
  - **C. Nuvio setup** — the addon list and the Home layout
  - **D. Ad blocking** (NextDNS, optional)
  - **E. Speed tweaks** (shorter animations, Google TV recommendations off)
  - **F. XGIMI power button** (full shutdown so the logo shows on every start) — XGIMI only
- Their first name for the greeting (optional).

## 1. Connect to the TV (needed for A, D, E, F)

1. Tell the user how to enable network debugging on the TV:
   Settings → Device Preferences → About → click **Build** 7 times → back → Developer options → turn on **USB debugging** and **Network debugging** (on Google TV: Settings → System → About → Android TV OS build).
2. On the PC, in the `windows` folder: run `get-adb.bat` (downloads Google's ADB), then `find-projector.bat`.
3. Copy `config.example.bat` to `config.bat` and set `IP=<address>:5555`.
4. First connection: the TV shows **"Allow network debugging?"** — the user ticks *Always allow* and presses **Allow**.

## 2. Collect keys (only for the parts chosen)

| Key | Needed for | How the user gets it |
|---|---|---|
| TMDB API key (v3) | A — streaming-service pages, logos | https://www.themoviedb.org/settings/api (free) |
| NextDNS profile ID | D | https://my.nextdns.io → new profile → the 6-character ID |
| NextDNS API key | A — "ads blocked today" counter (optional) | my.nextdns.io → Account → API |
| Cloudflare account | B | https://dash.cloudflare.com (free), user verifies their email |
| Nuvio account | B, C | they already have one |

Write them into `windows/config.bat` (`TMDB_KEY`, `NEXTDNS_ID`, `NEXTDNS_KEY`, `NAME`). Nothing else.

## 3. Live TV addon (B)

Follow `livetv-addon/README.md`:
1. In the user's browser: Cloudflare → Workers & Pages → Create → Hello World → name it → Deploy → Edit code → replace everything with `livetv-addon/worker-bundle.js` → Deploy.
2. Open `<worker-url>/configure`. Ask which countries/languages they want. If they chose A, tick **Add "Open TV Guide" link**.
3. Copy the addon link → add it in Nuvio (nuvio.tv → Account → Addons → Add Addon, or in the app).
4. Put the link without `/manifest.json` into `config.bat` as `LIVETV=`.

## 4. Horizon Home (A)

1. Get `HorizonHome.apk` from this repo's **Releases → latest** (or build it: `launcher/build.sh`, needs JDK 17 + Android SDK build-tools and platform 30). Put it in the `windows` folder.
2. Run `install-horizon.bat`. It installs the app, grants its permissions, backs up the current home screen and Home-button setting, makes Horizon the home screen, and passes the keys from `config.bat`.
3. On the TV: if it asks **"Allow network debugging?"** again (Horizon uses it for the memory panel and the in-app projector settings), the user allows it.
4. Check with a screenshot. Undo any time: `use-original-home.bat` (or `uninstall-horizon.bat`).

What the user gets is described in the README. The Live TV tile is the first tile in "Streaming services".

## 5. Nuvio setup (C)

1. Walk through `nuvio/addons.md` with the user. For each addon with a configure page, open it in their browser, let them sign in or enter keys, and add the resulting link to their Nuvio account in that order. Skip what they don't want.
2. Home layout: in their browser at https://personalized.virtualpavi.com, recreate the collections in `nuvio/collections-template.json` (see `nuvio/README.md`), then **Push**. Ask before pushing: it replaces the collections currently in their Nuvio account.

## 6. Optional tweaks

- D: `ad-block-on.bat` (undo `ad-block-off.bat`)
- E: `speed-up.bat` (undo `undo-speed-up.bat`)
- F: `xgimi-power-full-off.bat` (undo `xgimi-power-standby.bat`)

## 7. Finish

Tell the user what was installed, where the undo scripts are, and that the `backup` folder next to the scripts must be kept for the undo scripts to work.
