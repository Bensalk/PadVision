# Security

## Reporting a problem

If you find a security issue in PadVision, please **do not open a public issue**.
Use GitHub's private reporting instead: **Security → Report a vulnerability** on this repository.
You'll get a reply as soon as possible, and a fix will be released before the details are made public.

## What the app can and cannot do

- The app requires INTERNET permission to stream live TV channels and load M3U playlists.
- Settings (playlists, favourites, theme, profiles) stay in the app's private storage.
- Recordings are saved to the app's external Movies folder only. No other files are written to the device.
- No data is sent to any server operated by the app: all network requests go to the M3U sources and stream URLs you configure.

## Release signing

- Releases are built and signed by GitHub Actions. The signing key is stored only in encrypted GitHub Secrets, is unpacked only for the signing step and deleted right after. It is not in this repository or its history.
