# Nuvio addon setup (template)

The order below is the order to install them in Nuvio. Nuvio asks addons top to bottom, so put metadata and catalogs first and stream sources after.

Every addon with a **Configure** page gives you your *own* link (often with your API keys inside it).
**Never share those links or screenshots of your addon list.**

Where to add them: on a phone or PC, nuvio.tv → Account → Addons → **Add Addon**. Or on the TV: Settings → Media → Content & Discovery → Addons.

| # | Addon | What it does | Where to get your link | Keys you need |
|---|---|---|---|---|
| 1 | **Nuvio Personalized** | Your Home rows: For You, Because you watched…, Streaming-service folders, Genres | https://personalized.virtualpavi.com → sign in with your Nuvio account → **Copy addon link** | none (links to your Nuvio account) |
| 2 | **Bingecat** | Extra discovery catalogs | https://bingecat.com | free account |
| 3 | **Bingecat AI Search** | Search by description ("heist movie set in Mumbai") | same site, the search addon | free account |
| 4 | **Top 10 Today** | Daily top-10 rows | `https://posters-rank.vercel.app/manifest.json` | none |
| 5 | **AIOMetadata** | Better titles, logos, posters, ratings (replaces most of Cinemeta) | https://aiometadata.viren070.me → configure | TMDB key (free), optional MDBList / Trakt |
| 6 | **AIOStreams** | All your stream sources in one sorted list | an AIOStreams instance (e.g. ElfHosted, or a community instance) → configure | your stream service of choice |
| 7 | **Bharat Binge** | Indian catalogs (OTT top-10s, new Hindi/regional releases) | `https://bharat-binge.semi-column.workers.dev/manifest.json` (has a configure page) | none |
| 8 | **OpenSubtitles PRO** | Subtitles (set English + Hindi, AI translation on) | https://opensubtitlesv3-pro.dexter21767.com → configure | none |
| 9 | **Cinemeta** | Fallback metadata | `https://v3-cinemeta.strem.io/manifest.json` | none |
| 10 | **BetterPosters** / **Posters+** | Posters with rating badges | btttr.cc / postersplus.slokker.cc → configure | TMDB key, MDBList key |
| 11 | **MDBList lists** | Curated rows (IMAX, 3D, Indian IMAX…) | https://mdblist.com → your lists → Stremio addon link | MDBList key (free) |
| 12 | **YouStream** | YouTube channels/playlists inside Nuvio | youstream.up.railway.app → configure | none |
| 13 | **Live TV (this kit)** | Live channels for every country + TV guide + "Open TV Guide" button | your own Worker's `/configure` page (see `livetv-addon/`) | free Cloudflare account |

Tips
- Keep **one** metadata addon in charge (AIOMetadata above Cinemeta).
- Keep **one** subtitles addon (OpenSubtitles PRO).
- Put stream sources inside AIOStreams instead of installing many stream addons separately — one list, de-duplicated, best quality first.
- On a low-memory TV (1–2 GB), fewer Home rows = faster Nuvio. 25–30 rows is plenty.
