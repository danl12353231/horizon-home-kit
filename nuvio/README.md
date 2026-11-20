# Nuvio Home layout (Nuvio Personalized)

`collections-template.json` is my Nuvio Home layout, exported from Nuvio Personalized and cleaned:

- 5 pinned collections: **Discover** (8 folders), **Streaming Services** (22), **Genres** (21), **Anime** (5), **Formats** (2)
- each folder keeps its title, tile shape, artwork and the catalogs it shows
- every private link, key and account id has been removed. Each catalog says which addon it comes from (`source`) and its catalog id.

It is a **blueprint**, not a file you can import directly: the catalogs belong to addons that each person installs with their own link.

## Rebuilding it on your account

1. Install the addons from `addons.md` first (at least Nuvio Personalized, AIOStreams, Bharat Binge, MDBList).
2. Open https://personalized.virtualpavi.com, sign in with your Nuvio account.
3. Collections → **New**, recreate each collection and folder from the template: same title, tile shape, artwork links, then pick the matching catalogs from your installed addons (`source` tells you which addon).
4. Home order → arrange, then **Push** to Nuvio.

Claude can do steps 2–4 for you in the browser (see `../SETUP-WITH-CLAUDE.md`).

`source` names in the template:

| source | addon |
|---|---|
| `aiostreams-kamino` | an AIOStreams instance with catalogs turned on (Indian language catalogs, apnetv) |
| `bharat-binge` | Bharat Binge |
| `nuvio-personalized` | Nuvio Personalized (its own rows, regenerated for your account) |
| `mdblist` | MDBList list addon |
