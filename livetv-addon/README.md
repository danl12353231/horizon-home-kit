# Live TV addon for Nuvio / Stremio

Live channels from every country with a matched TV guide, as a Nuvio/Stremio addon, plus a playlist link for TiviMate / OTT Navigator.

- ~15,500 channels in ~180 countries, ~7,500 with a TV guide
- sources: iptv-org, a validated list, Free-TV, a health-checked list, Samsung TV Plus, Pluto TV, Roku Channel
- every link is test-played every 3 hours; working + sharpest links come first
- rows in Nuvio: On now, Starting soon, Tonight, Channels, per-category rows; channel pages show the schedule as episodes
- optional **📺 TV GUIDE** link that opens Horizon Home's full-screen guide

The channel and guide data is rebuilt by GitHub Actions in [jyotiprakash101/nuvio-livetv](https://github.com/jyotiprakash101/nuvio-livetv) and read straight from GitHub. You only host the small addon server (a free Cloudflare Worker), so every user has their own and nobody hits anyone else's limits.

## Deploy your own (5 minutes, free)

1. Sign up at https://dash.cloudflare.com (free plan) and verify your email.
2. **Workers & Pages → Create → Start with Hello World**, name it e.g. `my-livetv`, **Deploy**.
3. **Edit code**, select everything, paste the whole of [`worker-bundle.js`](worker-bundle.js), **Deploy**.
4. Open `https://my-livetv.<your-subdomain>.workers.dev/configure`
   - pick countries, categories, languages, sources
   - using Horizon Home? tick **Add "Open TV Guide" link**
   - **Copy addon link** → paste into Nuvio (Addons → Add Addon)
5. For Horizon Home, put that link (without `/manifest.json`) in `windows/config.bat` as `LIVETV=`.

Using Wrangler instead: `npx wrangler deploy` in this folder (uses `wrangler.toml`, `worker/src/index.js`).

Want your own data too (different sources, your own schedule)? Fork `nuvio-livetv`, let its Action run, and change the two `raw.githubusercontent.com/.../nuvio-livetv/data/` lines in `worker-bundle.js` (and `GuideActivity.RAW` in the launcher) to your fork.

The addon hosts no video. Streams are public links listed by the projects above.
