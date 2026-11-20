@echo off
rem ---- Copy this file to config.bat and fill it in (Claude does this for you). ----
rem Projector address with port, e.g. 192.168.1.20:5555  (find-projector.bat finds it)
set "IP=192.168.1.20:5555"
rem Name shown in the greeting (optional, one word)
set "NAME="
rem TMDB API key (free, https://www.themoviedb.org/settings/api) - streaming-service pages and logos
set "TMDB_KEY="
rem Your own Live TV addon link from your Cloudflare Worker, without /manifest.json (optional)
set "LIVETV="
rem NextDNS profile ID like abc123 (optional, for ad blocking)
set "NEXTDNS_ID="
rem NextDNS API key (optional, only for the blocked-ads counter on the home screen)
set "NEXTDNS_KEY="
