# TuneStacker 🎶
A Java Android Music Downloader and Player Application.

*This app was made as a personal project of mine.*

<br/>

This app's primary functionality stems from [youtubedl-android](https://github.com/yausername/youtubedl-android) by [yausername](https://github.com/yausername).

<br/>

**Supported Sites:**

[List of supported Urls](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md)

## Features

**Audio Downloader**
 - Allows the user to input a video or audio url and download it directly into the app.

**Playlist Imports and Sync**
 - This feature allows the user import a given Youtube playlist and sync it with the app if the playlist changes. More platforms for playlists are planned for the future.

**Playlist Creation and Management**
 - Gives the users the ability to create, manage, and even merge playlists together.
 
 **Built-in Media Player**
 - The app comes with a media player to play the downloaded songs and playlists. Just like with most other media players, it supports shuffle play.

**Playlist and Song Exporting**
 - Songs and Playlists can be exported, placing the files into the device's **MUSIC** directory.
 
<br/>
 
## Preview
![preview_1](https://user-images.githubusercontent.com/93726181/212566991-810dcdbb-f8cc-42e2-81fc-272a169151d7.PNG) 

![preview_4](https://user-images.githubusercontent.com/93726181/212567103-c09d4648-d8eb-4221-928f-862c72be041a.PNG)

<br/>

## Setup

**API Keys**

API keys must be supplied for the various platforms that the app supports.
 - [Youtube API](https://developers.google.com/youtube/v3)

After obtaining the key(s), they must be set in your project root's `local.properties` file:

```
# Youtube API v3
YT_API_KEY=<insert>
```

<br/>
 
Disclaimer
----
Download links from Spotify are currently not working, but compatability is planned in the upcoming future.
